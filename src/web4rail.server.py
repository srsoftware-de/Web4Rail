#!/usr/bin/python

import socket,sys,os
from thread import *

class web4rail_server:
	system = None

	def __init__(self,port):
		self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		print 'Socket created.'

		try:
			self.socket.bind(('',port))
		except socket.error as msg:
			print 'Bind failed. Error ('+str(msg[0])+'): '+str(msg[1])
			sys.exit(-1)

		print 'Bound to socket at port '+str(port)

	def client(self,conn):
		if self.system == None:
			self.select_system(conn)
		conn.close

	def create_system(self,path):
		print 'creating new system at '+path
		os.mkdir(path, 0755)
		file = open(path+'/plan.json', 'w+')
		
	def load_system(self,path,conn):
		print 'loading system from '+path
		with open(path+'/plan.json', 'r') as plan_file:
			plan=plan_file.read()
		conn.send("PLAN:\n")
		conn.send(plan+"\n")

	def select_system(self,conn):
		path='/'
		conn.send("Welcome to the Web2Rail server. Please select a SYSTEM first:\n");
		while True:
			conn.send('current dir: '+path+"\n")
			contents = sorted(os.listdir(path))
			
			conn.send("select one from\n")
			for entry in contents:
				conn.send('  '+entry+"\n")
			conn.send("--\n")
			entry = conn.recv(1024).strip()
			if entry in contents:
				if os.path.isfile(path+entry):
					break
				path += entry+'/'
			else:
				conn.send(path+entry+' does not exist. Create (yes/no/abort)?\n')
				input = conn.recv(1024).strip()
				if input == 'yes':
					path += entry
					self.create_system(path)
					break
				if input == 'abort':
					path = None
					break
				path = '/'
								
		if path == None:
			return
		
		self.load_system(path,conn)


	def start(self):
		self.socket.listen(10)
		print 'Server started.'

		while True:
			conn, add = self.socket.accept()
			start_new_thread(self.client,(conn,))

if len(sys.argv) == 2:
	port = int(sys.argv[1])
else:
	port = 7965

server = web4rail_server(port)
server.start()
