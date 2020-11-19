const ADD = 'add';
const BODY = 'body';
const CU = 'cu';
const DIV = 'DIV';
const MOVE = 'move';
const OPAC = 100;
const PLAN = '#plan';
const POST = 'POST';
const PROPS = 'props';
const SQUARE = 30;
const SVG = 'svg';
var selected = null;
var mode = null;
var messageTimer = null;
var messageOpacity = 0;
var trainAwaitingDestination = null;

function addClass(data){
	parts = data.split(" ");
	$('#'+parts[0]).addClass(parts[1]);
}

function addMessage(txt){
	$('#messages').html(txt);
	if (messageTimer != null) window.clearInterval(messageTimer);
	messageOpacity = 3000;
	messageTimer = setInterval(fadeMessage,100);		
}

function addTile(x,y){	
	return request({realm:'plan',action:mode,tile:selected.id,x:x,y:y});
}

function clickTile(x,y){
	var id = x+"-"+y;
	var tiles = $('#'+id);
	if (tiles.length > 0) {
		if (trainAwaitingDestination != null && tiles.hasClass("Block")) {
			request({realm:'train',id:trainAwaitingDestination,action:MOVE,destination:id});
			trainAwaitingDestination = null;
			$(PLAN).css('cursor','');
			return false;
		}
		request({realm:'plan',action:'click',id:id});
	}
	return false;
}


function closeMenu(ev){
	if (selected != null) $(selected).css('border','');
	$('.menu .list').css('display','')
	mode = null;
	selected = null;
	return false;
}

function closeWindows(){
	$('.window').remove();
	$('#plan').css('height','').css('width','');
}

function connectCu(){
	return request({realm:CU,action:"connect"});
}

function dropClass(data){
	var parts = data.split(" ");
	for (var i=1; i<parts.length; i++) $('#'+parts[0]).removeClass(parts[i]);
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
//	console.log('enableMove:',ev);
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

function fadeMessage(){
	messageOpacity -= 10;
	if (messageOpacity < 1) window.clearInterval(messageTimer);
	var o = messageOpacity;
	if (o>OPAC) o=OPAC;	
	$('#messages').css('opacity',o/OPAC);
}

function getCookie(key) {
    var keyValue = document.cookie.match('(^|;) ?' + key + '=([^;]*)(;|$)');
    return keyValue ? keyValue[2] : null;
}

function heartbeat(data){
	$('#heartbeat').show().fadeOut(2000);
	return false;
}

function keypress(ev){
	if (ev.code === 'Enter') request({realm:"cu",action:"emergency"});
}

function moveTile(x,y){	
	var id = x+"-"+y;
	return request({realm:'plan',action:mode,direction:selected.id,id:id});
}

function place(data){
	var tag = $(data);
	$('#'+tag.attr('id')).remove();
	$('#scroll').append(tag);
	return false;
}

function planClick(ev){
	//console.log('planClick:',ev);
	var plan=$('#scroll').get(0);
	var x = Math.floor((plan.scrollLeft+ev.clientX)/SQUARE);
	var y = Math.floor((plan.scrollTop+ev.clientY)/SQUARE);

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

function remove(id){
	$('#'+id).remove();
	return false;
}

function request(data){
	$.ajax({
		url : 'plan',
		method : POST,
		data : data,
		success: function(resp){
			if (data.realm != 'car' && data.realm != 'loco') closeWindows();
			if (resp.startsWith('<html')) return;
			if (resp.startsWith('<svg')){
				place(resp);				
			} else if (resp.startsWith('<')) {
				var isWindow = $(resp).attr('class') == 'window';
				if (isWindow) $('.window').remove();
				$(BODY).append($(resp));			
				if (isWindow) tileWindow();
			} else {
				addMessage(resp);
			}
		}
	});
	return false;	
}

function runAction(ev){
	var clicked = ev.target;
	var realm = clicked.hasAttribute('class') ? clicked.getAttribute('class') : null;
	console.log("runAction: ",{action: clicked.id, realm:realm});
	if (clicked.id == 'qrcode'){
		window.open("https://api.qrserver.com/v1/create-qr-code/?data="+window.location.href,'_blank');
	} else if (clicked.id == 'fullscreen'){
		toggleFullscreen();
	} else {
		return request({action:ev.target.id,realm:realm}); // TODO: ask for name
	}
	return false;
}

function selectDest(trainId){
	trainAwaitingDestination = trainId;
	closeWindows();
	$(PLAN).css('cursor','help');
	return false;
}

function stream(ev){
	var data = ev.data;
	console.log("received: ",data);
	if (data.startsWith('<svg')) return place(data);
	if (data.startsWith("heartbeat")) return heartbeat(data);
	if (data.startsWith("place ")) return place(data.substring(6));
	if (data.startsWith("remove")) return remove(data.substring(7));
	if (data.startsWith("addclass")) return addClass(data.substring(9));
	if (data.startsWith("dropclass")) return dropClass(data.substring(10));
	
	if (data.startsWith("<div") && $(data).attr('class') == 'window'){
		$('.window').remove();
		$(BODY).append($(data));
		tileWindow();
		return;
	}			

	addMessage(data);
}

function submitForm(formId){
	return request($('#'+formId).serialize());
}

function swapTiling(ev){
	var vertical = getCookie("tiling") == 'v';
	document.cookie = "tiling="+(vertical?'h':'v');
	tileWindow();
}

function tileWindow(){
	var vertical = getCookie("tiling") == 'v';
	var width = vertical ? '50%':'';
	var height = vertical ? '' : '50%';
	$(PLAN).css('width',width).css('height',height);
	$('.window').css('width',width).css('height',height);
	$('.swapbtn').text(vertical ? '⇩' : '⇨');
}

function toggleFullscreen(){
	if (document.fullscreenElement == null){
		document.documentElement.requestFullscreen();
	} else document.exitFullscreen();
}

window.onload = function () {
	var isDragging = false;
	$('.menu > div').click(closeMenu);
	$('.menu .addtile .list svg').click(enableAdding);
	$('.menu .move .list div').click(enableMove);
	$('.menu .actions .list > div').click(runAction);
	$('.menu .trains .list > div').click(runAction);
	$('.menu .hardware .list > div').click(runAction);
	$(PLAN).click(planClick);
	$(document).keyup(keypress);
	(new EventSource("stream")).onmessage = stream;
}
