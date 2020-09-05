# -*- coding: utf-8 -*-
import gtk


class Plan():
	def load(self,filename):
		file = open(filename,'r')
		lines = file.readlines()
		print(lines)


dialog = gtk.FileChooserDialog("Open...",
             None,
             gtk.FILE_CHOOSER_ACTION_OPEN,
             (gtk.STOCK_CANCEL, gtk.RESPONSE_CANCEL, gtk.STOCK_OPEN, gtk.RESPONSE_OK))

response = dialog.run();

if response == gtk.RESPONSE_OK:
	plan = Plan();
	plan.load(dialog.get_filename())
dialog.destroy()
