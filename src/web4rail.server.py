#!/usr/bin/python

import socket
import sys
import os
from thread import *

class web4rail_server:
	system = None
	halt = False

	def __init__(self, port):
		self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
		print('Socket created.')

		try:
			self.socket.bind(('', port))
		except socket.error as msg:
			print('Bind failed. Error (' + str(msg[0]) + '): ' + str(msg[1]))
			sys.exit(-1)

		print('Bound to socket at port ' + str(port))

	def client(self, conn):
		if self.system is None:
			self.system = self.select_system(conn)
		self.load_system(conn)
		while True:
			response = conn.recv(1024).strip()
			if response == 'UPDATE PLAN':
				self.update_plan(conn)
			elif response == 'EXIT':
				break
			else:
				print(response)
		os._exit(0)

	# creates a new json file at the selected dir
	def create_system(self, path):
		print('creating new system at ' + path)
		os.mkdir(path, 0o755)
		json_file = open(path + '/plan.json', 'w+')
		json_file.close()

	def load_system(self, conn):
		print('loading system from ' + self.system)
		with open(self.system + '/plan.json', 'r') as plan_file:
			plan = plan_file.read()
		conn.send("PLAN:\n")
		conn.send(plan + "\n")

	def select_system(self, conn):
		path = '/home/srichter/workspace/Web4Rail/'
		conn.send("Welcome to the Web2Rail server. Please select a SYSTEM first:\n")
		while True:
			conn.send('current dir: ' + path + "\n")
			contents = sorted(os.listdir(path))

			conn.send("select one from\n")
			for entry in contents:
				conn.send('  ' + entry + "\n")
			conn.send("--\n")
			entry = conn.recv(1024).strip()
			if entry in contents:
				if os.path.isfile(path + entry):
					break
				path += entry + '/'
			else:
				conn.send(path + entry + ' does not exist. Create (yes/no/abort)?\n')
				response = conn.recv(1024).strip()
				if response == 'yes':
					path += entry
					self.create_system(path)
					break
				if response == 'abort':
					path = None
					break
				path = '/'

		if path is None:
			return

		return path

	def start(self):
		self.socket.listen(10)
		print('Server started.')

		while True:
			conn, add = self.socket.accept()
			start_new_thread(self.client, (conn,))
		print('Closing server')

	def update_plan(self,conn):
		json = conn.recv(32768).strip()
		json_file = open(self.system + '/plan.json', 'w')
		json_file.write(json)
		json_file.close()

if len(sys.argv) == 2:
	port = int(sys.argv[1])
else:
	port = 7668

server = web4rail_server(port)
server.start()
