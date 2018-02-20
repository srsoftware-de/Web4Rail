#!/usr/bin/python
import gi
gi.require_version('Gtk', '3.0')
from gi.repository import Gdk, Gtk

class Tile(Gtk.DrawingArea):
	def __init__(self):
		Gtk.DrawingArea.__init__(self)
		self.connect('draw',self.draw)
		self.set_size_request(32, 32)
		self.add_events(Gdk.EventMask.BUTTON_PRESS_MASK)
		
	def draw(self,widget,canvas):
		pass
		
	def connections(self):
		return (False,False,False,False)
		
	def connects_up(self):
		return self.connections()[0]
	
	def connects_down(self):
		return self.connections()[2]
		
	def connects_right(self):
		return self.connections()[1]
	
	def connects_left(self):
		return self.connections()[3]

class StraigthH(Tile):
	def draw(self,widget,cr):
		cr.set_source_rgb(0,0,0)
		cr.rectangle(0,11,32,10)
		cr.fill()
		
	def connections(self):
		return (False,True,False,True)
		
class StraigthV(Tile):
	def draw(self,widget,cr):
		cr.set_source_rgb(0,0,0)
		cr.rectangle(11,0,10,32)
		cr.fill()
		
	def connections(self):
		return (True,False,True,False)
		
class Diag_TL(Tile):
	def draw(self,widget,cr):
		cr.set_source_rgb(0,0,0)
		cr.set_line_width(7)
		cr.move_to(-5,21)
		cr.line_to(21,-5)
		cr.stroke()
		
	def connections(self):
		return (True,False,False,True)

class Diag_BR(Tile):
	def draw(self,widget,cr):
		cr.set_source_rgb(0,0,0)
		cr.set_line_width(7)
		cr.move_to(12,37)
		cr.line_to(37,12)
		cr.stroke()
		
	def connections(self):
		return (False,True,True,False)

class TO_BRL(Tile):
	def draw(self,widget,cr):
		cr.set_source_rgb(0,0,0)
		cr.set_line_width(7)
		cr.move_to(12,37)
		cr.line_to(37,12)
		cr.stroke()
		cr.rectangle(0,11,32,10)
		cr.fill()
		
	def connections(self):
		return (False,True,True,True)
