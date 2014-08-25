
Daem0n {

	classvar <>currentOp;
	classvar <>opQueue;
	classvar <>unleashed;
	classvar window;
	classvar unleashButton;
	classvar <>period;

	*version { ^"9 August 2014"; }

	*execute { |x|
		thisProcess.interpreter.executeFile(x);
	}

	*summon {
		| initialPeriod = 0.25 |
		period = initialPeriod;
		if(not(Main.versionAtLeast(3,7)), {
				"*** ALERT: using old SuperCollider version ***".postln;
		});
		opQueue = [];
		Tdef(\daem0n, { loop { Daem0n.daem0n; period.wait;}}).play(quant:1);
		window = Window.new("Daem0n",Rect(200,200,100,50));
		unleashButton = Button.new(window,Rect(10,10,80,30)).action = { |x|
			switch(x.value,
				1,{Daem0n.unleash},
				0,{Daem0n.leash});
		};
		unleashButton.states_([['unleash'],['leash']]);
		window.front;
		Document.current.text = Document.current.text ++ "\n";
		Daem0n.unleash;
		^"summoned";
	}

	*unleash {
		unleashed = true;
		unleashButton.value = 1;
	}

	*leash {
		unleashed = false;
		unleashButton.value = 0;
	}

	*dismiss {
		Tdef(\daem0n.clear);
		fork {
			while( {Document.current.text.size>0} ,{
				var text = Document.current.text;
				var pos = text.size.rand;
				text.removeAt(pos);
				Document.current.text = text;
				0.0625.wait;
			});
			Server.default.stopRecording;
			Server.default.quit;
		};
	}

	*await {
		|f|
		var synth = { SendPeakRMS.kr(Ndef(\in).ar,50,3,'/wait'); }.play;
		OSCdef(\wait,{
			|msg|
			if(msg[3]>0.5,{
				OSCdef(\wait).clear;
				f.value;
				synth.free;
			});
		},"/wait");
	}

	*append {
		|x|
		opQueue = opQueue.add(["append",x,x]);
		Tdef(\daem0n).play(quant:1);
	}

	*appendOp {
		if(currentOp[2].notEmpty,{
			Document.current.text = Document.current.text ++ currentOp[2].first;
			currentOp[2] = currentOp[2].drop(1);
			if(currentOp[2].isEmpty, {
				Document.current.text = Document.current.text ++ "\n";
				currentOp[1].interpret;
				currentOp = nil;
			});
		});
	}

	*deleteKey {
		|key|
		opQueue = opQueue.add(["deleteKey",key]);
		Tdef(\daem0n).play(quant:1);
	}

	*deleteKeyOp {
		var text = Document.current.text;
		if(currentOp[2].isNil,{ // first dequeue action on this op
			var a,b,c,t;
			a = text.find(currentOp[1]) + currentOp[1].size;
			currentOp.add(text.findAll("\n").select({|x|x>a}).at(0) - 1);
			currentOp.add(currentOp[2] - a + 1);
		});
		text.removeAt(currentOp[2]);
		Document.current.text = text;
		currentOp[2] = currentOp[2] - 1;
		currentOp[3] = currentOp[3] - 1;
		if(currentOp[3]<1,{ currentOp = nil; });
	}

	*appendKey {
		|key,value|
		opQueue = opQueue.add(["appendKey",key,value]);
		Tdef(\daem0n).play(quant:1);
	}

	*appendKeyOp {
		var text = Document.current.text;
		if(currentOp[3].isNil,{ // first dequeue action on this op
			currentOp = currentOp.add(currentOp[2]);
			currentOp = currentOp.add(text.find(currentOp[1])+currentOp[1].size);
		});
		if(currentOp[3].notEmpty,{
			text = text.insert(currentOp[4],currentOp[3].first);
			Document.current.text = text;
			currentOp[3] = currentOp[3].drop(1);
			currentOp[4] = currentOp[4] + 1;
		},{
			Document.current.text = Document.current.text; //++ "\n";
			(currentOp[1] ++ currentOp[2]).interpret;
			currentOp = nil;
		});
	}

	*appendReplace {
		| begin,end |
		if(Document.current.text.find(begin).notNil,{
			Daem0n.deleteKey(begin);
			Daem0n.appendKey(begin,end);
		},{
			Daem0n.append(begin ++ end);
		});
	}

	*three {
		| class,key,value |
		var begin = class ++ "(" ++ Daem0n.asString(key) ++ ",";
		var end = Daem0n.asString(value) ++ ");";
		Daem0n.appendReplace(begin,end);
	}

	*ndef {
		| key,value |
		Daem0n.three("Ndef",key,value);
	}

	*pbindef {
		| key1,key2,value |
		var begin = "Pbindef(" ++ Daem0n.asString(key1) ++ "," ++ Daem0n.asString(key2) ++ ",";
		var end = Daem0n.asString(value) ++ ");";
		if(Document.current.text.find(begin).notNil,{
			Daem0n.deleteKey(begin);
			Daem0n.appendKey(begin,end);
		},{
			Daem0n.append(begin ++ end);
		});
	}

	*pbindef2 {
		| key1,key2,value2,key3,value3 |
		var begin = "Pbindef(" ++ Daem0n.asString(key1) ++ "," ++ Daem0n.asString(key2) ++ ",";
		var end = Daem0n.asString(value2) ++ "," ++ Daem0n.asString(key3) ++ "," ++ Daem0n.asString(value3) ++ ");";
		if(Document.current.text.find(begin).notNil,{
			Daem0n.deleteKey(begin);
			Daem0n.appendKey(begin,end);
		},{
			Daem0n.append(begin ++ end);
		});
	}

	*asString {
		| x |
		^if(x.isString,x,x.asCompileString);
	}

	*playPbindef {
		| key,quant=nil |
		if(quant.notNil,
			{ Daem0n.append("Pbindef(" ++ Daem0n.asString(key) ++ ").play(quant:"++quant.asCompileString++");"); },
			{ Daem0n.append("Pbindef(" ++ Daem0n.asString(key) ++ ").play;"); }
		);
	}

	*pdefn {
		| key,value |
		var begin = "Pdefn(" ++ Daem0n.asString(key) ++ ",";
		if(Document.current.text.find(begin).notNil,{
			Daem0n.deleteKey(begin);
			Daem0n.appendKey(begin,Daem0n.asString(value) ++ ");");
		},{
			Daem0n.append(begin ++ Daem0n.asString(value) ++ ");");
		});
	}

	*function {
		|f|
		opQueue = opQueue.add(["function",f]);
		Tdef(\daem0n).play(quant:1);
	}

	*functionOp {
		currentOp[1].value;
		currentOp = nil;
	}

	*spell {
		|path|
		var ops = File(path,"r").readAllString.split(Char.nl);
		ops.collect( { |op|
			("appending: " + op).postln;
			Daem0n.append(op);
		} );
		^((ops.size).asString ++ " ops");
	}

	*daem0n {
		if(currentOp.notNil,{ // if there is a current op, advance a step on it
			if(currentOp[0] == "append", { Daem0n.appendOp; ^this; });
			if(currentOp[0] == "deleteKey", { Daem0n.deleteKeyOp; ^this; });
			if(currentOp[0] == "appendKey", { Daem0n.appendKeyOp; ^this; });
			if(currentOp[0] == "function", { Daem0n.functionOp; ^this; });

		},{ // if there is no current op, check to see if a new op can be dequeued
			if(opQueue.notEmpty,{
				currentOp = opQueue.first;
				opQueue = opQueue.drop(1);
			},{
				if(unleashed.notNil,{
					if(unleashed==true,{
						~spell.value;
					});
				});
			});
		});
	}


	*appendPdefn {
		| key,pattern |
		Daem0n.append( "Pdefn(\\" ++ key ++ "," ++ pattern.asCompileString ++ ");");
	}

	*scramble {
		| period,n |
		fork {
			var text = Document.current.text;
			n.do {
				Document.current.text = Document.current.text.scramble;
				period.wait;
			};
			Document.current.text = text;
		};
	}

	*degrade {
		| period,n |
		fork {
			var text = Document.current.text;
			n.do {
				Document.current.text = Document.current.text.put(text.size.rand,Char.comma);
				period.wait;
			};
			Document.current.text = text;
		};

	}

}