This repository contains all the code in order to change the OpenNebula installation.

The "haizea" directory contains the haizea code with the modifications in order to be compatible with OpenNebula 3.2 and for the new scheduler policy.

The "haizea_policies" directory contains the two policies implemented (algorithm 3 for policies_simple.py and algorithm 4 for policies_repl.py).

The "deployment_module" directory contains all the java code of the deployment module. It is used for the hybrid clone case and is the one after replication has been added.

The "jar" directory contains all the *.jar files needed for the deployment module.

The "python" directory contains the code to retrieve the host's activity. probe_client.py is needed for the haizea policies to retrieve the host's activity and host_server.py must be installed on each host to collect the activity.