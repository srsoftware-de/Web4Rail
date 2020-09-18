const ADD = 'add';
const MOVE = 'move';
const SQUARE = 30;
const BODY = '#plan';
const DIV = 'DIV';
const SVG = 'svg';
const PLAN = 'plan';
const POST = 'POST';
var selected = null;
var mode = null;

function addMessage(txt){
	$('#messages').html(txt).show().delay(1000).fadeOut();
}

function addTile(x,y){	
	console.log("addTile:",selected.id,x,y);
	$.ajax({
		url : PLAN,
		method: POST,
		data : {action:mode,tile:selected.id,x:x,y:y},
		success: function(resp){
			var id = 'tile-'+x+'-'+y;
			$('#'+id).remove();
			var tile = $(selected).clone().css({left:(30*x)+'px',top:(30*y)+'px','border':''}).attr('id',id);
			if (selected.id != 'Eraser') $(BODY).append(tile);
			addMessage(resp);
		}
	});
}

function bodyClick(ev){
	console.log('bodyClick:',ev);
	var x = Math.floor(ev.clientX/SQUARE);
	var y = Math.floor(ev.clientY/SQUARE);

	switch (mode){
		case undefined:
		case null:
			return clickTile(x,y);
		case ADD:
			return addTile(x,y);
		case MOVE:
			return moveTile(x,y);
	}
	console.log('unknown action "'+mode+'" @ ('+ev.clientX+','+ev.clientY+')');
}

function clickTile(x,y){
	console.log("clickTile:",x,y);
	if ($('#tile-'+x+'-'+y).length > 0) request({action:'openProps',x:x,y:y});
	return false;
}


function closeMenu(ev){
	console.log('closeMenu:',ev);
	if (selected != null) $(selected).css('border','');
	$('.menu .list').css('display','')
	mode = null;
	selected = null;
	return false;
}

function closeWindows(){
	$('.window').remove();
}

function enableAdding(ev){
//	console.log('enableAdding:',ev);
	if (selected != null) $(selected).css('border','');
	selected = ev.target;
	while (selected != null && selected.nodeName != SVG) selected = selected.parentNode;
	if (selected == null){
		mode = null;
	} else {
		$(selected).css('border','2px solid red');
		$('.menu .addtile .list').css('display','inherit');
		mode = ADD;
	}
	return false; // otherwise body.click would also be triggered
}

function enableMove(ev){
	console.log('enableMove:',ev);
	if (selected != null) $(selected).css('border','');
	selected = ev.target;
	while (selected != null && selected.nodeName != DIV) selected = selected.parentNode;
	if (selected == null){
		mode = null;
	} else {
		$(selected).css('border','2px solid red');
		$('.menu .move .list').css('display','inherit');
		mode = MOVE;
	}
	return false; // otherwise body.click would also be triggered
}

function moveTile(x,y){	
	console.log("moveTile:",selected.id,x,y);
	$.ajax({
		url : PLAN,
		method: POST,
		data : {action:mode,direction:selected.id,x:x,y:y},
		success: function(resp){
			if (resp.startsWith('<')){
				$(resp).each(function(){
					if (this.id != undefined){
						$('#'+this.id).remove();
						$(BODY).append($(this));
					}
				});
				$('#tile-'+x+'-'+y).remove();
			} else {
				addMessage(resp);
			}
		}
	});
}

function openRoute(id){
	request({action:'openRoute',id:id});
	return false;
}

function request(data){
	$.ajax({
		url : PLAN,
		method : POST,
		data : data,
		success: function(resp){
			closeWindows();
			if (resp.startsWith('<')){
				$('body').append($(resp));
			} else {
				addMessage(resp);
			}
		}
	});	
}

function runAction(ev){
	console.log("runAction: ",ev.target.id);
	$.ajax({
		url : PLAN,
		method : POST,
		data : {action:ev.target.id,file:'default'}, // TODO: ask for name
		success: function(resp){ addMessage(resp);}
	});
	return false;
}

window.onload = function () {
	var isDragging = false;
	console.log($(BODY).each(function(){console.log(this)}));
	$('.menu > div').click(closeMenu);
	$('.menu .addtile .list svg').click(enableAdding);
	$('.menu .move .list div').click(enableMove);
	$('.menu .actions .list > div').click(runAction);
	$(BODY).click(bodyClick);
	var stream = new EventSource("stream");
	stream.onmessage = function(ev){
		console.log(ev);
	}
}
