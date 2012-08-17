from haizea.core.scheduler.policy import HostSelectionPolicy
import logging

class MyPolicy(HostSelectionPolicy):
	def __init__(self, slottable):
		HostSelectionPolicy.__init__(self, slottable)
		self.logger = logging.getLogger("ENACT.ONE.INFO")

	def get_host_score(self, node, time, lease):
		node_details = self.slottable.nodes[node]
		for storage in lease.storages:
			if not storage in node_details.storages:
				self.logger.info("VM %d with storages [%s] has received a score of %d on node %d with storages [%s]" % (lease.id, ",".join(lease.storages),0, node, ",".join(node_details.storages)))
				return 0
		self.logger.info("VM %d with storages [%s] has received a score of %d on node %d with storages [%s]" % (lease.id, ",".join(lease.storages),1, node, ",".join(node_details.storages)))
		return 1
