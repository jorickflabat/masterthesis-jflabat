#!/usr/bin/python

"""
Implementes a server that must be running on the hosts
"""
import socket
import threading
import os
import sys
import httplib
import time
import subprocess
import re

"""
Thread that will regularly probe the host to update values
"""
class probethread(threading.Thread):
	
	def __init__(self):
		threading.Thread.__init__(self)
		self.iostat = (0,0)
		self.ifstat = (0,0)
		self.top = (0,0)
		self.runn = 0
	
	def run(self):
		print "Start probing host..."
		while True:
			#self.runn = runn_vms()
			self.iostat = make_iostat_fio()
			self.ifstat = make_ifstat()
			self.top = make_top()
			time.sleep(90)
			

"""
Return the ip address of this machine
"""
def get_host_ip():
    for ip in socket.gethostbyname_ex(socket.gethostname())[2]:
        if not ip.startswith("127."):
            return ip
        
"""
Make a http request to a specific host
"""   
def make_http_request(host, ip, port, sock):
    start_time = time.time()
    sleep_total = 0
    while True:
        try:
            conn = httplib.HTTPConnection(host)
            conn.request("GET", "")
            res = conn.getresponse()
            if res.reason == "OK":
                end_time = time.time()
                #print "%s: %fs" % (host, end_time - start_time)
                print "%s: %fs send to (%s,%d)" % (host, end_time - start_time, ip, port)
                sock.sendto("%s: %fs" % (host, end_time - start_time), (ip, port))
                return
        except:
            time.sleep(1)
            sleep_total = sleep_total + 1
            if sleep_total > 1000:
            	return
            
"""
Replace , by .
"""
def comma_to_dot(s):
	new_s = ""
	for c in s:
		if c == ",":
			new_s = new_s + "."
		else:
			new_s = new_s + c
	return new_s

"""
Parse the informations of the iostat line
"""
def parse_io(line):
	read_done = False
	segs = line.split()
	read = float(comma_to_dot(segs[2]))
	write = float(comma_to_dot(segs[3]))
	return (read,write)
	
"""
Parse the informations of the fio line
"""
def parse_io_fio(line):
	segs = line.split()
	aggr = segs[2]
	read = aggr[6:]
	while len(read) > 0:
		if parse_number(read[-1])[0]:
			return read
		else:
			read = read[0:-1]
	
"""
Parse the informations of the ifstat line
"""
def parse_if(line):
	segs = line.split()
	down = float(segs[0])
	up = float(segs[1])
	return (down, up)
	
"""
Return true if the line is the good one in the metric file
"""
def is_good_io(line):
	splitted = line.split()
	if len(splitted) == 0:
		return False
	token = splitted[0]
	if token == DISK:
		return True
	else:
		return False
		
"""
Return (true, type) if the line is the good one in the metric file using FIO
"""
def is_good_io_fio(line):
	splitted = line.split()
	if len(splitted) == 0:
		return (False, None)
	token = splitted[0]
	if token == "READ:" or token == "WRITE:":
		return (True, token[:-1])
	else:
		return (False, None)
		
"""
Check if the string is a float number, if it's a number, the float
value will be in the second part of the tuple
"""
def parse_number(s):
	try:
		num = float(s)
		return (True, num)
	except ValueError:
		return (False, 0)
		
"""
Get the # of running vms
"""
def runn_vms():
	out = subprocess.Popen(['virsh', '-c', 'qemu:///system', 'list'], stdout=subprocess.PIPE)
	nb = 0
	for line in out.stdout:
		splitted = line.split()
		if len(splitted) == 0:
			continue
		if parse_number(splitted[0])[0] and splitted[2] == "running":
			nb = nb + 1
	return nb

"""
Make the iostat command
"""
def make_iostat():
	iostat = subprocess.Popen(['iostat', '-k', '60', '2'], stdout=subprocess.PIPE)
	first = True
	for line in iostat.stdout:
		line = line.strip()
		if is_good_io(line):
			if not first:
				return parse_io(line)
			else:
				first=False
		else:
			pass
	return (0, 0)
	
"""
Make the iostat with fio
"""
def make_iostat_fio():
	iostat = subprocess.Popen(['fio', 'io_probe.fio'], stdout=subprocess.PIPE)
	read = 0
	write = 0
	for line in iostat.stdout:
		line = line.strip()
		res = is_good_io_fio(line)
		if res[0]:
			print res[1]
			if res[1] == "READ":
				temp = parse_number(parse_io_fio(line))
				if temp[0]:
					read = temp[1]
				print read
				continue
			else:
				temp = parse_number(parse_io_fio(line))
				if temp[0]:
					write = temp[1]
				print write
				return (read, write)
		else:
			pass
	return (read, write)
	
"""
Make the ifstat command
"""
def make_ifstat():
	ifstat = subprocess.Popen(['ifstat', '1', '1'], stdout=subprocess.PIPE)
	i = 0
	for line in ifstat.stdout:
		if i == 2:
			return parse_if(line)
		else:
			i = i + 1
	return (0, 0)
	
"""
Make the top command
"""
def make_top():
	top = subprocess.Popen(['top', '-bin2'], stdout=subprocess.PIPE)
	for line in top.stdout:
		if re.match("^Cpu", line):
			for el in line[7:-1].split(","):
				temp = el.strip().split("%")
				if temp[1]=="id":
					idle = temp[0]
					freecpu = float(idle) * float(TOTALCPU) / 100
					usedcpu = float(TOTALCPU) - freecpu
					break
	return (freecpu, usedcpu)
            
if __name__ == "__main__":
	if len(sys.argv) != 2:
		IP = get_host_ip()
	else:
		IP = sys.argv[1]
		
	INFO = True
	DEBUG = False
	PORT = 5050
	VM_PORT = 6050
	HOSTNAME = socket.gethostname()
	DISK = "sda"
	TOTALCPU = 200

	CLEAR_CACHE = "echo 3 > /proc/sys/vm/drop_caches"

	sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	sock.bind((IP, PORT))

	print "Start HostServer(%s)" % (IP)
	
	try:
		probe = probethread()
		probe.daemon = True
		probe.start()

		while True:
			data, addr = sock.recvfrom(1024)
	
			#Prepare message
			split = data.split(":")
			if split[0] == "relay":
				next_host = split[1]
				msg = ":".join(split[2:])
	        
				#Send message
				out = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
				out.sendto(msg, (next_host, VM_PORT))
			
				if INFO:
					print "relay \"%s\" to %s" %(msg,next_host)
			elif split[0] == "clear":
				if INFO:
					print "cache cleared"
				os.system(CLEAR_CACHE)
			elif split[0] == "httpprobe":
				if INFO:
					print "http request to %s" % split[1]
				sendback_port = int(split[2])
				thread = threading.Thread(None, make_http_request, None, (), {'host':split[1], 'ip':addr[0], 'port':sendback_port, 'sock':sock})
				thread.start()
			elif data == "probe":
				if INFO:
					print "probe received from ", addr
				io = probe.iostat
				ifs = probe.ifstat
				top = probe.top
				vms = probe.runn
				if DEBUG:
					print io
					print ifs
					print top
				msg = "%s:%f:%f:%f:%f:%f:%f:%d" % (HOSTNAME, io[0], io[1], ifs[0], ifs[1], top[0], top[1], vms)
				sock.sendto(msg, addr)
				if INFO:	
					print "probe send to ", addr
			elif data == "ping":
				print "ping received from ", addr
				sock.sendto("pong", addr)
			else:
				print data
	except(KeyboardInterrupt, SystemExit):
		print '\n! Received keyboard interrupt, quitting threads.\n'

