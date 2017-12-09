process.title = 'Daem0nToTidal';
var stderr = process.stderr;
var osc = require('osc');

if(false) {
  stderr.write("immortal mode - exceptions will be ignored (use during critical performances, not during development/practice)\n");
  process.on('uncaughtException', function(err) {
    // do nothing on uncaught exceptions in order to hopefully
    // not disrupt a time-limited performance that is in progress
  });
}

var udp = new osc.UDPPort( { localAddress: "0.0.0.0", localPort: 7999 });
if(udp!=null)udp.open();
udp.on('message', function(m) {
  if(m.args.length != 1) {
    console.log("error: received OSC with number of arguments !=1");
    return;
  }
  console.log(m.args[0]);
});
