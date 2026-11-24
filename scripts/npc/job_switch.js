// NPC selection is advisory; the server revalidates family, state and SP at confirmation.
var status = 0;
var target = 0;
var originalJob = 0;
var swapStats = false;
var expectedStr = 0;
var expectedDex = 0;
var jobs = [];
var names = [];
var targetName = "";
var groups = [
    [[110,120,130], ["剑客","准骑士","枪战士"]],
    [[210,220,230], ["火毒法师","冰雷法师","牧师"]],
    [[310,320], ["猎人","弩弓手"]],
    [[410,420], ["刺客","侠客"]],
    [[510,520], ["拳手","火枪手"]]
];

function start() {
    originalJob = cm.getJobId();
    for (var i = 0; i < groups.length; i++) {
        if (groups[i][0].indexOf(Math.floor(originalJob / 10) * 10) >= 0) {
            jobs = groups[i][0]; names = groups[i][1]; break;
        }
    }
    if (jobs.length == 0 || originalJob % 10 > 2 || cm.getLevel() < 30) {
        cm.sendOk("[JobSwitch]" + "仅开放 30 级以上冒险家同一大类内的分支互换，保持当前转数。");
        cm.dispose(); return;
    }
    var text = "切换职业\r\n";
    for (var j = 0; j < jobs.length; j++)
        if (jobs[j] != Math.floor(originalJob / 10) * 10) text += "#L" + j + "#" + names[j] + "#l\r\n";
    cm.sendSimple("[JobSwitch]" + text);
}

function confirm() {
    expectedStr = cm.getPlayer().getStr();
    expectedDex = cm.getPlayer().getDex();
    status = 2;
    cm.sendYesNo("[JobSwitch]切换为 " + targetName + "？\r\n自动卸装 · 返还技能点"
        + (swapStats ? "\r\n力量 " + expectedStr + " → " + expectedDex + "，敏捷 " + expectedDex + " → " + expectedStr : ""));
}

function action(mode, type, selection) {
    if (mode != 1) { cm.dispose(); return; }
    if (status == 0) {
        if (selection < 0 || selection >= jobs.length || selection != Math.floor(selection)
                || jobs[selection] == Math.floor(originalJob / 10) * 10) { cm.dispose(); return; }
        target = jobs[selection] + originalJob % 10; targetName = names[selection] + "系（" + (originalJob % 10 + 2) + "转）";
        if (Math.floor(originalJob / 100) == 5) {
            status = 1;
            cm.sendSimple("[JobSwitch]" + "属性处理\r\n#L0#保留属性#l\r\n#L1#力量敏捷互换#l");
        } else confirm();
    } else if (status == 1) {
        if (selection !== 0 && selection !== 1) { cm.dispose(); return; }
        swapStats = selection == 1;
        confirm();
    } else if (status == 2) {
        status = 3;
        var result = cm.getPlayer().switchAdventurerBranch(target, originalJob, swapStats, expectedStr, expectedDex);
        cm.sendOk("[JobSwitch]" + (result.indexOf("切换成功") == 0 ? "切换成功" : result));
        cm.dispose();
    }
}
