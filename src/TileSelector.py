#!/usr/bin/python
import gi
gi.require_version('Gtk', '3.0')
from gi.repository import Gtk
from Tiles import *

class TileSelector(Gtk.Window):
	def __init__(self,connection):
		Gtk.Window.__init__(self)
		self.connect("destroy", Gtk.main_quit)
		
		grid = Gtk.Grid()
		
		straight_h = StraigthH()
		straight_h.connect('button-press-event',self.select)
		
		straight_v = StraigthV()
		straight_v.connect('button-press-event',self.select)
		
		diag_tl = Diag_TL()
		diag_tl.connect('button-press-event',self.select)
		
		diag_br = Diag_BR()
		diag_br.connect('button-press-event',self.select)
		
		to_brl = TO_BRL()
		to_brl.connect('button-press-event',self.select)

		grid.attach(straight_h,0,0,1,1)
		grid.attach(straight_v,1,0,1,1)
		grid.attach(diag_tl,0,1,1,1)
		grid.attach(diag_br,1,1,1,1)
		grid.attach(to_brl,1,2,1,1)
		
		self.add(grid)
		
		self.show_all()
		Gtk.main()
		
	def select(self,btn,evt):
		self.tile = btn.__class__
		self.hide()
		Gtk.main_quit()