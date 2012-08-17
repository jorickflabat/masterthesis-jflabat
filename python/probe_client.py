#!/usr/bin/python

import socket

IP = "192.168.1.8"
PORT = 15050

"""
Parse a string into float
"""
def to_float(s):
	try:
		return float(s)
	except ValueError:
		return float("NaN")

class IO_Client:
	
	"""
	Define a new IO probe client
		input hosts_ip: dictionary "<hostname> => <ip_address>"
	"""
	def __init__(self, hosts_ip = {}, timeout = 30):
		socket.setdefaulttimeout(timeout)
		self.hosts_ip = {}
		self.metrics = {}
		self.id = 0
		for host in self.hosts_ip.keys():
			self.add_host(host, hosts_ip[host])
			
	"""
	Add a new host to the client
	"""
	def add_host(self, host, ip):
		self.hosts_ip[host] = ip
		self.metrics[host] = {}
		self.metrics[host]['id'] = self.id
		self.metrics[host]['read'] = 0
		self.metrics[host]['write'] = 0
		self.metrics[host]['down'] = 0
		self.metrics[host]['up'] = 0
		self.metrics[host]['freecpu'] = 0
		self.metrics[host]['usedcpu'] = 0
		self.metrics[host]['vms'] = 0
		
	"""
	Remove a host from the client
	"""
	def remove_host(self, host):
		del self.hosts_ip[host]
		del self.metrics[host]
	
	"""
	Get read metric for a host
	"""	
	def get_read(self, host):
		return self.metrics[host]['read']
		
	"""
	Get write metric for a host
	"""	
	def get_write(self, host):
		return self.metrics[host]['write']
		
	"""
	Get download rate for a host
	"""
	def get_down(self, host):
		return self.metrics[host]['down']
		
	"""
	Get upload rate for a host
	"""
	def get_up(self, host):
		return self.metrics[host]['up']
		
	"""
	Get freecpu for a host
	"""
	def get_freecpu(self, host):
		return self.metrics[host]['freecpu']
		
	"""
	Get usedcpu for a host
	"""
	def get_usedcpu(self, host):
		return self.metrics[host]['usedcpu']
		
	"""
	Get the # of running vms
	"""
	def get_running_vms(self, host):
		return self.metrics[host]['vms']
		
	"""
	Get probe identifier
	"""
	def get_id(self, host):
		return self.metrics[host]['id']
	
	"""
	Show metrics
	"""
	def show_metrics(self):
		for host in self.metrics.keys():
			print "%s:" % host
			print "   Read: %f KB/s" % self.get_read(host)
			print "   Write: %f KB/s" % self.get_write(host)
			print "   Down: %f KB/s" % self.get_down(host)
			print "   Up: %f KB/s" % self.get_up(host)
			print "   Freecpu: %f" % self.get_freecpu(host)
			print "   Usedcpu: %f" % self.get_usedcpu(host)
			print "   Running vms: %d" % self.get_running_vms(host)
	
	"""
	Probe all the hosts to get the new io metrics
	"""
	def probe_hosts(self):
		msg = "probe"

		sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
		sock.bind((IP, PORT))
	
		for host in self.hosts_ip.keys():
			print msg, self.hosts_ip[host], 5050
			sock.sendto(msg, (self.hosts_ip[host], 5050))
		
		for i in range(len(self.hosts_ip)):
			try:
				data, addr = sock.recvfrom(1024)
				segs = data.split(":")
				if not segs[0] in self.metrics:
					print "No %s in the metrics table" % segs[0]
				else:
					host = segs[0]
					self.metrics[host]['read'] = to_float(segs[1])
					self.metrics[host]['write'] = to_float(segs[2])
					self.metrics[host]['down'] = to_float(segs[3])
					self.metrics[host]['up'] = to_float(segs[4])
					self.metrics[host]['freecpu'] = to_float(segs[5])
					self.metrics[host]['usedcpu'] = to_float(segs[6])
					self.metrics[host]['vms'] = int(segs[7])
					self.metrics[host]['id'] = self.id + 1
			except socket.timeout:
				pass
		for host in self.hosts_ip.keys():
			if self.metrics[host]['id'] == self.id:
				self.metrics[host]['read'] = float("NaN")
				self.metrics[host]['write'] = float("NaN")
				self.metrics[host]['down'] = float("NaN")
				self.metrics[host]['up'] = float("NaN")
				self.metrics[host]['freecpu'] = float("NaN")
				self.metrics[host]['usedcpu'] = float("NaN")
				self.metrics[host]['vms'] = int("NaN")
				self.metrics[host]['id'] = self.id + 1
				print "Nothing received for %s" % host
		self.id = self.id + 1

client = IO_Client(timeout=20)
client.add_host("hercules", "192.168.0.8")
client.probe_hosts()
client.show_metrics()