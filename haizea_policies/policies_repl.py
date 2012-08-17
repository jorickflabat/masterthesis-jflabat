from haizea.core.scheduler.policy import HostSelectionPolicy
import logging
import probe_client

max_read = 1000.0
max_write = 1000.0

read_thresh = 600
write_thresh = 600

local_KO = (0.1,0.5)
local_OK = (0.1, 1.0)
network_KO = (0.0,0.4)
network_OK = (0.0, 0.9)

class MyPolicy(HostSelectionPolicy):
	def __init__(self, slottable):
		HostSelectionPolicy.__init__(self, slottable)
		self.logger = logging.getLogger("ENACT.ONE.INFO")
		self.probe = probe_client.IO_Client(timeout=20)
		self.probe.add_host("hercules", "192.168.0.8")
		self.probe.add_host("gemini", "192.168.0.26")
		self.probe.add_host("bootes", "192.168.0.34")
		self.volumes={ "BOHE":[1,2], "BEGE":[1,3], "GEHE":[2,3]}

	def get_host_score(self, node, time, lease):
		node_details = self.slottable.nodes[node]
		local = self.make_host_list_local(lease.storages[0])
		network = self.make_host_list_network(lease.storages[0])
                #calculate ratio for the host
                self.probe.probe_hosts()
                read = self.probe.get_read(node_details.hostname)
                write = self.probe.get_write(node_details.hostname)
                perc = self.compute_io_percentage(read, write)
                self.logger.info("For %s, the ratio is %f for io read %f and io write %f" % (node_details.hostname, perc, read, write))
		if lease.clone and not lease.save:
			# we consider it has IO loaded
			if node_details in local:
				return (local_OK[1] - local_OK[0]) * perc + local_OK[0]
			elif node_details in network:
				return (network_OK[1] - network_OK[0]) * perc + network_OK[0]
			else:
				return 0
                elif lease.clone and lease.save:
                        max = 0
			for storage in self.slottable.nodes[node].storages.keys():
				if self.slottable.nodes[node].storages[storage]=='local':
					node_o = self.other_host_for_storage(node, storage)
					#self.logger.info("node %d" % (node))
					#self.logger.info("hey %s" % self.slottable.nodes[node_o].hostname)
					read_o = self.probe.get_read(self.slottable.nodes[node_o].hostname)
					write_o = self.probe.get_write(self.slottable.nodes[node_o].hostname)
					perc_o = self.compute_io_percentage(read_o, write_o)
					ratio = (perc + perc_o)/2.0
					score = 0
					vol = ""
					if node_details in local:
						score = (local_OK[1] - local_OK[0]) * ratio + local_OK[0]
					elif node_details in network:
						score = (network_OK[1] - network_OK[0]) * ratio + network_OK[0]
					else:
						score = 0
					self.logger.info("score %f for %s and node %s" % (score, storage, node))
					if score > max:
						max = score
						vol = storage
			self.logger.info("Max score for %s with %s score:%s " % (node, vol, max)) 
			return max				
		else:
			#The images are not cloned, the best is to deploy on the host that holds the image
			if len(lease.storages) == 0:
				return 0
			else:
				# The host holds the image locally
				if node_details in local:
					self.logger.info("For %s, the score given is %f" % (node_details.hostname, 0.5 * perc + 0.5))
					return 0.5 * perc + 0.5
				# The host can access the image by network
				elif node_details in network:
					self.logger.info("For %s, the score given is %f" % (node_details.hostname, 0.5 * perc))
					return 0.5
			# The image is not accessible on this host
			return 0

	def make_host_list_local(self, storage):
		node_list = []
		for node in self.slottable.nodes.keys():
			node_details = self.slottable.nodes[node]
			if storage in node_details.storages.keys() and node_details.storages[storage]=='local':
				node_list.append(node_details)
		return node_list	
	
	def make_host_list_network(self, storage):
		node_list = []
		for node in self.slottable.nodes.keys():
			node_details = self.slottable.nodes[node]
			if storage in node_details.storages.keys() and node_details.storages[storage]=='network':
				node_list.append(node_details)
		return node_list

	def compute_io_percentage(self, read, write):
		r = read / max_read
		w = write / max_write
		perc = (r + w)/2.0
		if perc > 1:
			return 1
		else:
			return perc

	def other_host_for_storage(self, node, storage):
		if self.volumes[storage][0] == node:
			return self.volumes[storage][1]
		else:
			return self.volumes[storage][0]
       
        def compute_storage_host(self):		
		self.volumes = {}
		for node in range(1, len(self.slottable.nodes)+1):
			node_details = self.slottable.nodes[node]
			for storage in node_details.storages.keys():
				if node_details.storages[storage] == 'local':
					if not storage in self.volumes:
						self.volumes[storage] = []
					self.volumes[storage].append(node)
		for vol in self.volumes.keys():
			self.logger.info("volume %s" % (vol))
			for node in self.volumes[vol]:
				self.logger.info("%s" % node)

