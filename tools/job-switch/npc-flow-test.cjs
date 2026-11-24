// Run from Cosmic: node tools/job-switch/npc-flow-test.cjs
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync('scripts/npc/job_switch.js', 'utf8');
function dialog(job = 110, level = 40) {
    const calls = [];
    const messages = [];
    const cm = {
        getJobId: () => job, getLevel: () => level,
        sendSimple: text => { calls.push('menu'); messages.push(text); },
        sendYesNo: text => { calls.push('confirm'); messages.push(text); },
        sendOk: text => calls.push(text),
        dispose: () => calls.push('dispose'),
        getPlayer: () => ({getStr: () => 300, getDex: () => 40,
            switchAdventurerBranch: (...args) => { calls.push(args); return 'result'; }
        })
    };
    const context = vm.createContext({cm});
    vm.runInContext(source, context);
    context.start();
    return {calls, messages, action: context.action};
}
for (const invalid of [-1, 0, 3, 0.5, 999, NaN]) {
    const d = dialog(); d.action(1, 0, invalid);
    assert.deepEqual(d.calls, ['menu', 'dispose']);
}
for (const stage of [0, 1]) {
    const d = dialog();
    if (stage) d.action(1, 0, 1);
    d.action(0, 0, 0);
    assert.equal(d.calls.at(-1), 'dispose');
    assert.equal(d.calls.some(Array.isArray), false);
}
const groups = [[110,120,130],[210,220,230],[310,320],[410,420],[510,520]];
for (const group of groups) for (const job of group) {
    for (let index = 0; index < group.length; index++) if (group[index] !== job) {
        for (const swap of job >= 500 ? [false, true] : [false]) {
            const d = dialog(job);
            assert.equal(d.calls[0], 'menu');
            d.action(1, 0, index);
            if (job >= 500) {
                assert.deepEqual(d.calls, ['menu', 'menu']);
                d.action(1, 0, swap ? 1 : 0);
            }
            assert.equal(d.calls.at(-1), 'confirm');
            assert.equal(d.calls.some(Array.isArray), false);
            if (swap) assert.match(d.messages.at(-1), /力量 300 → 40，敏捷 40 → 300/);
            d.action(1, 0, 0); d.action(1, 0, 0);
            assert.deepEqual(d.calls.filter(Array.isArray), [[group[index], job, swap, 300, 40]]);
        }
    }
}
for (const job of [0,100,113,213,900,1110,2100]) {
    const d = dialog(job); assert.equal(d.calls.at(-1), 'dispose');
    assert.equal(d.calls.includes('menu'), false);
}
for (const level of [29]) {
    const d = dialog(220, level); assert.equal(d.calls.at(-1), 'dispose');
}
for (const step of [0,1,2]) {
    const d = dialog(510);
    if (step >= 1) d.action(1,0,1);
    if (step >= 2) d.action(1,0,1);
    d.action(0,0,0);
    assert.equal(d.calls.some(Array.isArray), false);
}
const invalidPirate = dialog(510);
invalidPirate.action(1,0,1); invalidPirate.action(1,0,2);
assert.equal(invalidPirate.calls.some(Array.isArray), false);
console.log('All 12 jobs: menu, same-family targets, cancel, confirmation, pirate stat preview and replay checks passed.');
