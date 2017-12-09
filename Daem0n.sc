
Daem0n {

	classvar <>currentOp;
	classvar <>opQueue;
	classvar <>unleashed;
	classvar window;
	classvar unleashButton;
	classvar <>period;

	classvar <>guitarBus,<>ampResponse;
	classvar <>cheby,<>proxySpace;
	classvar <>guitarSynth,<>mlSynth,<>mixSynth;
	classvar <>startTime,<>targetDuration,<>progress;
	classvar loudnessBuffer,centroidBuffer,flatnessBuffer;
	classvar <>loudness,<>centroid,<>flatness;
	classvar <>drone,<>rhythm,<>bebop;
	classvar <>volatility,<>intensity,<>complexity;
	classvar <>browser,<>tidal;

	classvar <>d1code,<>d1intensity,<>d1complexity;
	classvar <>d2code,<>d2intensity,<>d2complexity;
	classvar <>d3code,<>d3intensity,<>d3complexity;
	classvar <>d4code,<>d4intensity,<>d4complexity;
	classvar <>d5code,<>d5intensity,<>d5complexity;

	*version { ^"1 Dec 2017"; }

	*go {
		| dur=720 |
		loudness=0;
		centroid=0;
		flatness=0;
		drone=0;
		rhythm=0;
		bebop=0;
		volatility=0;
		intensity=0;
		complexity=0;
		targetDuration = dur;
		browser = NetAddr.new("127.0.0.1",8000);
		tidal = NetAddr.new("127.0.0.1",7999);
		this.busesAndGroups;
		this.nodes;
		this.playGuitarSynth;
		this.playMlSynth;
		this.playMixSynth;

		this.d1("silence");
		this.d2("silence");
		this.d3("silence");
		this.d4("silence");
		this.d5("");


		this.runLayer1;
		this.runLayer2;
		this.runLayer3;
	}

	*busesAndGroups {
		guitarBus = Bus.audio(Server.default,1);
	}

	*playGuitarSynth {
		if(guitarSynth.notNil,{ guitarSynth.free });
		if(cheby.isNil,{ cheby = Buffer.alloc(Server.default,16384); });
		cheby.cheby([1,0,0.5,0.5,0.25,0.125,0.125,0.125,0.125,0.125,0.125]);
		guitarSynth = SynthDef(\guitar,{
			arg out = 0;
			var guitar = SoundIn.ar(0);
			var distorted = Shaper.ar(cheby.bufnum,guitar*80.dbamp);
			Out.ar(guitarBus,distorted);
		}).play(addAction:\addToTail);
	}

	*playMlSynth {
		loudnessBuffer = Buffer.alloc(Server.default,1024,1);
		centroidBuffer = Buffer.alloc(Server.default,1024,1);
		flatnessBuffer = Buffer.alloc(Server.default,1024,1);
		if(mlSynth.notNil,{mlSynth.free});
		mlSynth = SynthDef(\ml,{
			var fft;
			var guitar = In.ar(guitarBus);
			var loudnessFFT = FFT(loudnessBuffer,guitar);
			var centroidFFT = FFT(centroidBuffer,guitar);
			var flatnessFFT = FFT(flatnessBuffer,guitar);
			var loudness = Lag.kr(Loudness.kr(loudnessFFT),lagTime:1.0);
			var centroid = Lag.kr(SpecCentroid.kr(centroidFFT),lagTime:1.0);
			var flatness = Lag.kr(SpecFlatness.kr(flatnessFFT),lagTime:1.0);
			SendReply.kr(Impulse.kr(10),"/ml",[loudness,centroid,flatness]);
		}).play(addAction:\addToTail);
	}

	*nodes {
		proxySpace = ProxySpace.new.push;
		proxySpace.fadeTime = 2;
		~guitar = -0;
		~superDirt = 0;
		~precomp = 0;
		~postcomp = 0;
	}


	*playMixSynth {
		ampResponse = Buffer.read(Server.default, "/Users/d0kt0r0/Desktop/Marshall1960A-G12Ms-C414-Cap-6in.wav");
		if(mixSynth.notNil,{mixSynth.free});
		mixSynth = SynthDef(\mix,{
			var guitar = In.ar(guitarBus);
			var guitarAmp = Convolution2.ar(guitar,ampResponse.bufnum,0,128, ~guitar.kr.dbamp*(-30.dbamp));
			var superDirt = In.ar(0,numChannels:2)*~superDirt.kr.dbamp;
			var mix = ([guitarAmp,guitarAmp] + superDirt)*~precomp.kr.dbamp*(7.dbamp);
			var limit = Compander.ar(mix,mix,thresh:-3.dbamp,slopeAbove:1/20,clampTime:0.020,relaxTime:0.020,mul:-6.5.dbamp*~postcomp.kr.dbamp);
			ReplaceOut.ar(0,limit);
		}).play(addAction:\addToTail);
	}


	*runLayer1 {
		startTime = Main.elapsedTime;
		OSCdef(\l1,{ |msg,time,addr,port|
			//msg.postln;
			loudness = msg[3].linlin(20,72,0,1);
			centroid = msg[4].linlin(3000,10000,0,1);
			flatness = msg[5];
			drone = (1-(this.distance3d(loudness,centroid,flatness,0.9,0.25,0.44))).squared;
			rhythm  = (1-(this.distance3d(loudness,centroid,flatness,0.85,0.2,0.4))).squared;
			bebop = (1-(this.distance3d(loudness,centroid,flatness,1,0.5,0.5))).squared;
			progress = ((Main.elapsedTime - startTime)/targetDuration).clip(0,1);
//			[loudness,centroid,flatness,drone,rhythm,bebop,progress].postln;
			browser.sendMsg("/all","daem0n","setLoudness",loudness);
			browser.sendMsg("/all","daem0n","setCentroid",centroid);
			browser.sendMsg("/all","daem0n","setFlatness",flatness);
			browser.sendMsg("/all","daem0n","setDrone",drone);
			browser.sendMsg("/all","daem0n","setRhythm",rhythm);
			browser.sendMsg("/all","daem0n","setBebop",bebop);
			browser.sendMsg("/all","daem0n","setProgress",progress);
		},"/ml");
	}

	*triangleMap {
		|start,peak,end,x|
		var y = 0;
		x = x.clip(0,1);
		if(x<start,{y=0});
		if((x>=start) && (x<peak),{y=x.linlin(start,peak,0,1)});
		if((x>=peak) && (x<end),{y=x.linlin(peak,end,1,0)});
		^y;
	}

	*asrMap {
		|start,peak1,peak2,end,x|
		var y = 0;
		x = x.clip(0,1);
		if(x<start,{y=0});
		if((x>=start) && (x<peak1),{y=x.linlin(start,peak1,0,1)});
		if((x>=peak1) && (x<peak2),{y=1});
		if((x>=peak2) && (x<end),{y=x.linlin(peak2,end,1,0)});
		^y;
	}

	*runLayer2 {
		Tdef(\l2,{ inf.do {
			volatility = ((this.triangleMap(0,0.9,1.0,progress) + drone)/2).clip(0,1);
			intensity = (( (1-bebop)+(1-rhythm)+(this.asrMap(0,0.35,0.95,1.0,progress)))/3).clip(0,1);
			complexity = this.triangleMap(0,0.65,1,progress).clip(0,1);
			browser.sendMsg("/all","daem0n","setVolatility",volatility);
			browser.sendMsg("/all","daem0n","setIntensity",intensity);
			browser.sendMsg("/all","daem0n","setComplexity",complexity);
			0.05.wait;
		}}).play;
	}

	*runLayer3 {

		Tdef(\l3,{ inf.do {
			var waitTime = (1-volatility)*10+0.5;
			var layer = 4.rand;
			[waitTime,layer].postln;
			waitTime.wait;
			if(layer == 0, {
				this.generateD1;
				this.d1(d1code);
			});
			if(layer == 1,{
				this.generateD2;
				this.d2(d2code);
			});
			if(layer == 2,{
				this.generateD3;
				this.d3(d3code);
			});
			if(layer == 3,{
				this.generateD4;
				this.d4(d4code);
			});
			if(layer == 4,{
				this.generateD5;
				this.d5(d5code);
			});
		}}).play;
	}

	*generateD1 {
		var sample;
		sample = "bass0";
		if(intensity <= 0.1,{sample="bass0"});
		if((intensity > 0.1) && (intensity <= 0.2),{sample = "bass0*2 ~"});
		if((intensity > 0.2) && (intensity <= 0.3),{sample = "bass0*2 drum:1"});
		if((intensity > 0.3) && (intensity <= 0.4),{sample = "bass0*2 drum:1"});
		if((intensity > 0.4) && (intensity <= 0.5),{sample = "bass0*4 ~"});
		if((intensity > 0.5) && (intensity <= 0.6),{sample = "bass0*2 drum:1"});
		if((intensity > 0.6) && (intensity <= 0.7),{sample = "bass0*2 [drum:1 drum:2]"});
		if((intensity > 0.7) && (intensity <= 0.8),{sample = "bass0*4? [drum:1 drum2]"});
		if((intensity > 0.8) && (intensity <= 0.9),{sample = "bass0*4? [[drum:1,bass2] drum2]"});
		if((intensity > 0.9),{sample = "bass0*4 [[drum:1,bass2] drum2]"});
		sample = "s \"" ++ sample ++ "\"";
		if((complexity > 0.1) && (complexity <= 0.2),{ sample = "brak $ " ++ sample;});
		if((complexity > 0.2) && (complexity <= 0.3),{ sample = "jux (brak) $ " ++ sample;});
		if((complexity > 0.3) && (complexity <= 0.4),{ sample = "every 4 (jux (fast 2)) $ " ++ sample;});
		if((complexity > 0.4) && (complexity <= 0.5),{ sample = "every 3 (jux (fast 2)) $ " ++ sample;});
		if((complexity > 0.5) && (complexity <= 0.6),{ sample = "every 2 (jux (fast 2)) $ " ++ sample;});
		if((complexity > 0.6) && (complexity <= 0.7),{ sample = "iter 4 $ " ++ sample;});
		if((complexity > 0.7) && (complexity <= 0.8),{ sample = "iter 8 $ " ++ sample;});
		if((complexity > 0.8) && (complexity <= 0.9),{ sample = "every 2 (jux (fast 2)) $ every 3 (brak) $ " ++ sample;});
		if((complexity > 0.9),{ sample = "iter $ every 2 (jux (fast 2)) $ every 3 (brak) $ " ++ sample;});
		if(progress > 0.9, {sample = "silence"});
		d1code = sample;
	}

	*generateD2 {
		var sample;
		sample = "~";
		if((intensity > 0.2) && (intensity <= 0.4),{sample = "bass*2"});
		if((intensity > 0.4) && (intensity <= 0.6),{sample = "bass*4"});
		if((intensity > 0.6) && (intensity <= 0.8),{sample = "bass*8?"});
		if((intensity > 0.8),{sample = "bass*16"});
		sample = "s \"" ++ sample ++ "\"";
		if((complexity > 0.1) && (complexity <= 0.2),{ sample = sample ++ " # up \"[0 4]/4\"";});
		if((complexity > 0.2) && (complexity <= 0.3),{ sample = sample ++ " # up \"[0 3 4 3]/4\"";});
		if((complexity > 0.3) && (complexity <= 0.4),{ sample = sample ++ " # up (\"[0 3 4 3]/4\" + \"12\")";});
		if((complexity > 0.4) && (complexity <= 0.5),{ sample = sample ++ " # up (\"[0 3 4 3]/4\" + \"[0 12]*2\")";});
		if((complexity > 0.5) && (complexity <= 0.6),{ sample = sample ++ " # up (\"[0 3 4 3]/4\" + \"[0 12]*4\")";});
		if((complexity > 0.6) && (complexity <= 0.7),{ sample = sample ++ " # up (\"[0 3 4 3]/4\" + \"[0 12]*8\")";});
		if((complexity > 0.7) && (complexity <= 0.8),{ sample = sample ++ " # up (\"[0 3 4 3]/4\" + \"[0 7 12 -2]*4\")";});
		if((complexity > 0.8),{ sample = sample ++ " # up (\"[0 3 4 3]/4\" + \"[0 7 12 -12]*16/9\")";});
		if(progress > 0.9, {sample = "silence"});
		d2code = sample;
	}

	*generateD3 {
		var sample;
		sample = "~";
		if((intensity > 0.4) && (intensity <= 0.6),{sample = "[drum*2 drum*4] ~"});
		if((intensity > 0.6) && (intensity <= 0.8),{sample = "[drum*2 drum*4]*2"});
		if((intensity > 0.8),{sample = "[drum*4 drum*4]*2"});
		sample = "s \"" ++ sample ++ "\"";
		if((complexity <=0.2),{ sample = sample ++ " # up \"24\" # n (run 6)"});
		if((complexity > 0.2) && (complexity <= 0.4),{ sample = sample ++ " # up \"24\" # n (run 6) # shape \"[0 0.5]*3/2\"";});
		if((complexity > 0.4) && (complexity <= 0.6),{ sample = sample ++ " # up \"24\" # n (run 6) # shape \"[0 0.25 0.5 0.75]*7/3\"";});
		if((complexity > 0.6) && (complexity <= 0.8),{ sample = sample ++ " # up \"24\" # n (run 6) # shape \"[0 0.25 0.5 0.75 1]*17/5\"";});
		if((complexity > 0.8),{ sample = sample ++ " # up \"24\" # n (run 6) # shape (fast 4 sawwave1)";});
		if(progress > 0.9, {sample = "silence"});
		d3code = sample;
	}

	*generateD4 {
		var sample = "~";
		var slow = 2.rand+3;
		if(progress > 0, {
			var r = 19.rand;
			sample = "~ tacscan:" ++ (r.asString);
		});
		sample = "slow " ++ (slow.asString) ++ " $ s \"" ++ sample ++ "\" # pan (fast 7 sawwave1)";
		if((complexity > 0.25) && (complexity <= 0.5), { sample = "jux (fast 2) $ " ++ sample });
		if((complexity > 0.5) && (complexity <= 0.5), { sample = "jux (striate 8) $ " ++ sample });
		if((complexity > 0.75), { sample = "jux (striate 8 . fast 2) $ " ++ sample });
		if(progress > 0.98, {sample = "silence"});
		d4code = sample;
	}

	*generateD5 {
	}

	*setText1 {
		|x|
		if(x.isNil,{x="nil"});
		browser.sendMsg("/all","daem0n","setText1",x);
	}

	*setText2 {
		|x|
		if(x.isNil,{x="nil"});
		browser.sendMsg("/all","daem0n","setText2",x);
	}

	*setText3 {
		|x|
		if(x.isNil,{x="nil"});
		browser.sendMsg("/all","daem0n","setText3",x);
	}

	*setText4 {
		|x|
		if(x.isNil,{x="nil"});
		browser.sendMsg("/all","daem0n","setText4",x);
	}

	*setText5 {
		|x|
		if(x.isNil,{x="nil"});
		browser.sendMsg("/all","daem0n","setText5",x);
	}

	*browserReset {
		browser.sendMsg("/load","daem0n","Daem0n.js");
		browser.sendMsg("/refresh","daem0n");
	}

	*executeTidal {
		|x|
		if(x.isNil,{x="hush"});
		tidal.sendMsg("/tidal",x);
	}

	*d1 {
		|x|
		var y;
		if(x.isNil,{x="silence"});
		y = "d1 $ " ++ x;
		this.setText1(y);
		this.executeTidal(y);
	}

	*d2 {
		|x|
		var y;
		if(x.isNil,{x="silence"});
		y = "d2 $ " ++ x;
		this.setText2(y);
		this.executeTidal(y);
	}

	*d3 {
		|x|
		var y;
		if(x.isNil,{x="silence"});
		y = "d3 $ " ++ x;
		this.setText3(y);
		this.executeTidal(y);
	}

	*d4 {
		|x|
		var y;
		if(x.isNil,{x="silence"});
		y = "d4 $ " ++ x;
		this.setText4(y);
		this.executeTidal(y);
	}

	*d5 {
		|x|
		if(x.isNil,{x=" "});
		this.setText5(x);
		x.interpret;
	}

	*cps {
		|x|
		if(x.isNil,{x=104/60});
		this.executeTidal("cps " ++ (x.asString) ++ "/4");
	}

	*distance2d {
		|x1,y1,x2,y2|
		^(((x2-x1).squared+(y2-y1).squared).sqrt).linlin(0,1.414213562,0,1);
	}

	*distance3d {
		|x1,y1,z1,x2,y2,z2|
		^(((x2-x1).squared+(y2-y1).squared+(z2-z1).squared).sqrt).linlin(0,1.7320508,0,1);
	}

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