#!/usr/bin/python
import socket
from ConnectDialog import *
from FileDialog import *
from TrackPlan import *
from YesNoDialog import *
	
def readline(socket):
	buffer = socket.recv(4096)
	buffering = True
	while buffering:
		if "\n" in buffer:
			(line, buffer) = buffer.split("\n", 1)
			yield line
		else:
			more = socket.recv(4096)
			if not more:
				buffering = False
			else:
				buffer += more
	if buffer:
		yield buffer
		
conn_dlg = ConnectDialog()
conn_dlg.run()

if conn_dlg.port != None:
	client_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	client_sock.connect((conn_dlg.host,conn_dlg.port))
	client = readline(client_sock)
	current_dir = ''
			
	while True:
		line = client.next()
		
		if line.startswith('current dir'):
			current_dir = line
			continue
			
		if line.startswith('select one'):
			file_dialog = FileDialog(line,current_dir)
			while True:
				line = client.next()
				if line.startswith('--'):
					break
				file_dialog.add_dir(line.strip())
			file_dialog.run()
			
			client_sock.send(file_dialog.dir+"\n")
			continue
			
		if line == 'PLAN:':
			json = client.next()
			plan = TrackPlan(json)
			plan.run()
			break
			continue
		
		if 'does not exist. Create' in line:
			dir = line.split('does not exist')[0].strip()
			dialog = YesNoDialog(line)
			if dialog.answer:
				client_sock.send("yes\n")
			else:
				client_sock.send("no\n")
			continue
			
		print line
print "Test"