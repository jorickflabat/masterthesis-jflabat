# -------------------------------------------------------------------------- #
# Copyright 2006-2009, University of Chicago                                 #
# Copyright 2008-2009, Distributed Systems Architecture Group, Universidad   #
# Complutense de Madrid (dsa-research.org)                                   #
#                                                                            #
# Licensed under the Apache License, Version 2.0 (the "License"); you may    #
# not use this file except in compliance with the License. You may obtain    #
# a copy of the License at                                                   #
#                                                                            #
# http://www.apache.org/licenses/LICENSE-2.0                                 #
#                                                                            #
# Unless required by applicable law or agreed to in writing, software        #
# distributed under the License is distributed on an "AS IS" BASIS,          #
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   #
# See the License for the specific language governing permissions and        #
# limitations under the License.                                             #
# -------------------------------------------------------------------------- #

from haizea.core.scheduler import EnactmentError
from haizea.core.leases import Capacity
from haizea.core.scheduler.resourcepool import ResourcePoolNode
from haizea.core.enact import ResourcePoolInfo, VMEnactment, DeploymentEnactment
from haizea.common.utils import get_config, OpenNebulaXMLRPCClientSingleton
from haizea.common.opennebula_xmlrpc import OpenNebulaVM, OpenNebulaHost
import haizea.common.constants as constants
import logging
from time import sleep

class OpenNebulaEnactmentError(EnactmentError):
    def __init__(self, method, msg):
        self.method = method
        self.msg = msg
        self.message = "Error when invoking '%s': %s" % (method, msg)

class OpenNebulaResourcePoolInfo(ResourcePoolInfo):
    
    def __init__(self):
        ResourcePoolInfo.__init__(self)
        self.logger = logging.getLogger("ENACT.ONE.INFO")

        self.rpc = OpenNebulaXMLRPCClientSingleton().client

        # Get information about nodes from OpenNebula
        self.max_nod_id = 0
        self.nodes = {}

        self.resource_types = []
        self.resource_types.append((constants.RES_CPU,1))
        self.resource_types.append((constants.RES_MEM,1))
        self.resource_types.append((constants.RES_DISK,1))
                    
        self.logger.info("Fetching nodes from OpenNebula")            
        self.__fetch_nodes()
        self.logger.info("Fetched %i nodes from OpenNebula" % len(self.nodes))            
        
    def refresh(self):
        return self.__fetch_nodes()
        
    def get_nodes(self):
        return self.nodes
    
    def get_resource_types(self):
        return self.resource_types

    def get_bandwidth(self):
        return 0
    
    def __fetch_nodes(self):
        new_nodes = []
        hosts = self.rpc.hostpool_info()
        hostnames = set([n.hostname for n in self.nodes.values()])
        for host in hosts:
            # CPU
            # OpenNebula reports each CPU as "100"
            # (so, a 4-core machine is reported as "400")
            # We need to convert this to a multi-instance
            # resource type in Haizea            
            cpu = host.max_cpu
            ncpu = cpu / 100
            enact_id = host.id                
            hostname = host.name
            
            # We want to skip nodes we're already aware of ...
            if hostname in hostnames:
                continue

            # ... and those in an error or disabled state ...
            if host.state in (OpenNebulaHost.STATE_ERROR, OpenNebulaHost.STATE_DISABLED):
                continue
            
            # ... and those were monitoring information is not yet available.
            if cpu == 0:
                self.logger.debug("Skipping node '%s' (monitoring information not yet available)" % hostname)
                continue
            
            self.max_nod_id += 1
            
            nod_id = self.max_nod_id
            capacity = Capacity([constants.RES_CPU, constants.RES_MEM, constants.RES_DISK])
            
            capacity.set_ninstances(constants.RES_CPU, ncpu)
            for i in range(ncpu):
                capacity.set_quantity_instance(constants.RES_CPU, i+1, 100)            
            
            # Memory. Must divide by 1024 to obtain quantity in MB
            capacity.set_quantity(constants.RES_MEM, host.max_mem / 1024.0)
            
            # Disk
            # OpenNebula doesn't report this correctly yet.
            # We set it to an arbitrarily high value.
            capacity.set_quantity(constants.RES_DISK, 80000)

            node = ResourcePoolNode(nod_id, hostname, capacity, host.storages, host.running_vms)
            node.enactment_info = enact_id
            self.nodes[nod_id] = node
            new_nodes.append(node)
            self.logger.debug("Fetched node %i %s Storages:[%s] | Running VMs: %d" % (node.id, node.hostname, ",".join(node.storages.keys()), node.running_vms))
        return new_nodes
        

class OpenNebulaVMEnactment(VMEnactment):
    def __init__(self):
        VMEnactment.__init__(self)
        self.logger = logging.getLogger("ENACT.ONE.VM")
        self.rpc = OpenNebulaXMLRPCClientSingleton().client

    def start(self, action):
        for vnode in action.vnodes:
            # Unpack action
            vid = action.vnodes[vnode].enactment_info
            hid = action.vnodes[vnode].pnode
            
            self.logger.debug("Sending request to start VM for L%iV%i (ONE: vid=%i, hid=%i)"
                         % (action.lease_haizea_id, vnode, vid, hid))

            try:
                self.rpc.vm_deploy(vid, hid)
                self.logger.debug("Request succesful.")
            except Exception, msg:
                raise OpenNebulaEnactmentError("vm.deploy", msg)
            
    def stop(self, action):
        for vnode in action.vnodes:
            # Unpack action
            vid = action.vnodes[vnode].enactment_info
            
            self.logger.debug("Sending request to shutdown VM for L%iV%i (ONE: vid=%i)"
                         % (action.lease_haizea_id, vnode, vid))

            try:
                self.rpc.vm_shutdown(vid)
                self.logger.debug("Request succesful.")
            except Exception, msg:
                raise OpenNebulaEnactmentError("vm.shutdown", msg)
            
            # Space out commands to avoid OpenNebula from getting saturated
            # TODO: We should spawn out a thread to do this, so Haizea isn't
            # blocking until all these commands end
            interval = get_config().get("enactment-overhead").seconds
            sleep(interval)

    def suspend(self, action):
        for vnode in action.vnodes:
            # Unpack action
            vid = action.vnodes[vnode].enactment_info
            
            self.logger.debug("Sending request to suspend VM for L%iV%i (ONE: vid=%i)"
                         % (action.lease_haizea_id, vnode, vid))

            try:
                self.rpc.vm_suspend(vid)
                self.logger.debug("Request succesful.")
            except Exception, msg:
                raise OpenNebulaEnactmentError("vm.suspend", msg)
            
            # Space out commands to avoid OpenNebula from getting saturated
            # TODO: We should spawn out a thread to do this, so Haizea isn't
            # blocking until all these commands end
            interval = get_config().get("enactment-overhead").seconds
            sleep(interval)
        
    def resume(self, action):
        for vnode in action.vnodes:
            # Unpack action
            vid = action.vnodes[vnode].enactment_info
            
            self.logger.debug("Sending request to resume VM for L%iV%i (ONE: vid=%i)"
                         % (action.lease_haizea_id, vnode, vid))

            try:
                self.rpc.vm_resume(vid)
                self.logger.debug("Request succesful.")
            except Exception, msg:
                raise OpenNebulaEnactmentError("vm.resume", msg)
            
            # Space out commands to avoid OpenNebula from getting saturated
            # TODO: We should spawn out a thread to do this, so Haizea isn't
            # blocking until all these commands end
            interval = get_config().get("enactment-overhead").seconds
            sleep(interval)

    def verify_suspend(self, action):
        result = 0
        for vnode in action.vnodes:
            # Unpack action
            vid = action.vnodes[vnode].enactment_info
            
            try:
                vm = self.rpc.vm_info(vid)   
                state = vm.state
                if state == OpenNebulaVM.STATE_SUSPENDED:
                    self.logger.debug("Suspend of L%iV%i correct (ONE vid=%i)." % (action.lease_haizea_id, vnode, vid))
                else:
                    self.logger.warning("ONE did not complete suspend of L%iV%i on time. State is %i. (ONE vid=%i)" % (action.lease_haizea_id, vnode, state, vid))
                    result = 1
            except Exception, msg:
                raise OpenNebulaEnactmentError("vm.info", msg)

        return result
        
    def verify_resume(self, action):
        result = 0
        for vnode in action.vnodes:
            # Unpack action
            vid = action.vnodes[vnode].enactment_info
            
            try:
                vm = self.rpc.vm_info(vid)   
                state = vm.state
                if state == OpenNebulaVM.STATE_ACTIVE:
                    self.logger.debug("Resume of L%iV%i correct (ONE vid=%i)." % (action.lease_haizea_id, vnode, vid))
                else:
                    self.logger.warning("ONE did not complete resume of L%iV%i on time. State is %i. (ONE vid=%i)" % (action.lease_haizea_id, vnode, state, vid))
                    result = 1
            except Exception, msg:
                raise OpenNebulaEnactmentError("vm.info", msg)

        return result        

class OpenNebulaDummyDeploymentEnactment(DeploymentEnactment):    
    def __init__(self):
        DeploymentEnactment.__init__(self)
            
    def get_aux_nodes(self):
        return [] 
            
    def resolve_to_file(self, lease_id, vnode, diskimage_id):
        return "/var/haizea/images/%s-L%iV%i" % (diskimage_id, lease_id, vnode)
