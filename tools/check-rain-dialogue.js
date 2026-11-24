const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync(process.argv[2] || 'scripts/npc/12101.js', 'utf8');
for (let round=0;round<5;round++) {
 const events=[];
 const cm={sendNext:t=>events.push(['next',t]),sendNextPrev:t=>events.push(['both',t]),dispose:()=>events.push(['end'])};
 const ctx=vm.createContext({cm});vm.runInContext(source,ctx);
 ctx.start(); assert.equal(events.at(-1)[0],'next');
 ctx.action(1,0,-1);assert.match(events.at(-1)[1],/Southperry/);
 ctx.action(0,0,-1);assert.match(events.at(-1)[1],/Amherst/);
 ctx.action(1,0,-1);ctx.action(1,0,-1);assert.match(events.at(-1)[1],/Perion/);
 ctx.action(0,0,-1);assert.match(events.at(-1)[1],/Southperry/);
 ctx.action(1,0,-1);ctx.action(1,0,-1);assert.equal(events.at(-1)[0],'end');
 ctx.start();ctx.action(-1,0,-1);assert.equal(events.at(-1)[0],'end');
}
console.log('PASS: Rain advances on first confirm, supports previous/next, finishes and reopens across five sessions');
