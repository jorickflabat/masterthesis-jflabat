This repository contains all the code in order to change the OpenNebula installation.

The "haizea" directory contains the haizea code with the modifications in order to be compatible with OpenNebula 3.2 and for the new scheduler policy.

The "haizea_policies" directory contains the two policies implemented (algorithm 3 for policies_simple.py and algorithm 4 for policies_repl.py).

The "deployment_module" directory contains all the java code of the deployment module. It is used for the hybrid clone case and is the one after replication has been added.

The "jar" directory contains all the *.jar files needed for the deployment module.

The "python" directory contains the code to retrieve the host's activity. probe_client.py is needed for the haizea policies to retrieve the host's activity and host_server.py must be installed on each host to collect the activity.

The "tm_commands" contain all the transfer manager scripts. They must be placed in the /usr/lib/one/tm_commands directory and referenced in /etc/one/one.conf.

VMTemp is an example of a VM template

HostTemp is an example of a Host template

++++++++++++++++++++++++++++
|The installation procedure|
++++++++++++++++++++++++++++

Install OpenNebula 3.2 by following the documentation on opennebula.org. The directory will be shared with ssh. So follow the ssh procedure. Set the transfer manager scripts

Install GlusterFS 3.2, probe the peers and create the replicated volumes in /data
	ex:
		mkdir /data/test-export on host1 and host2
		gluster volume create test-volume replica 2 transport tcp host1:/data/test-export host2:/data/test-export
		cluster volume start test-export
		mount -t glusterfs host1:test-volume /data/test on each host

Install the host server on each host:
-------------------------------------

On each host, starts the activity server:
	sudo sh host_server.py

Install the deployment module:
------------------------------

You need first to add the hosts with their id in OpenNebula and the replicated volumes in HaizeaSocket.java. 
Each host with it host id must added. Then for each volumes, you must define the two hosts that exports them. The volumes are located by default in /data, but if they are elsewhere, you should change /data/ in makeImageCopy(String src, String name, String storage) of GlusterFSTools.java by you want.
An example is given in the file. 

In OneClient.java, you need to define the credential for the OpenNebula connection.

Then compile and execute:
-------------------------

javac -cp org.opennebula.client.jar:xmlrpc-client-3.1.2.jar:xmlrpc-common-3.1.2.
jar:ws-commons-util-1.0.2.jar deployment_module/*

java -cp org.opennebula.client.jar:xmlrpc-client-3.1.2.jar:xmlrpc-common-3.1.2.j
ar:ws-commons-util-1.0.2.jar:deployment_module HaizeaListenServer

Install and start haizea with the policies

Install haizea:
---------------

	sh setup.py

The haizea policy must be defined in the haizea.conf. The policies need probe_client.py where you must change the IP variable to the front-end's IP
Then start haizea:
	haizea -c haizea.conf



You can now install the host on OpenNebula with the transfer manager scripts and start using it. Don't forget to change the attributes of the hosts so that it knows the storages.


	

