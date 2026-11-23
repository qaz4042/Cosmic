package scripting;

import client.Character;
import org.junit.jupiter.api.Test;
import scripting.npc.NPCConversationManager;

import javax.script.Invocable;
import javax.script.ScriptEngine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/** Exercises the real GraalJS host boundary, including Java Integer selection arguments. */
class JobSwitchDialogTest extends AbstractScriptManager {
    private Invocable dialog(NPCConversationManager cm) throws Exception {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        ScriptEngine engine = getInvocableScriptEngine("npc/job_switch.js");
        assertNotNull(engine);
        engine.put("cm", cm);
        Invocable script = (Invocable) engine;
        script.invokeFunction("start");
        return script;
    }

    @Test void pirateConfirmsBothStatOptionsThroughJavaHostCalls() throws Exception {
        for (boolean swap : new boolean[]{false, true}) {
            NPCConversationManager cm = mock(NPCConversationManager.class);
            Character player = mock(Character.class);
            when(cm.getPlayer()).thenReturn(player);
            when(cm.getJobId()).thenReturn(510);
            when(cm.getLevel()).thenReturn(40);
            when(player.getStr()).thenReturn(300);
            when(player.getDex()).thenReturn(40);
            when(player.switchAdventurerBranch(520, 510, swap, 300, 40)).thenReturn("ok");
            Invocable script = dialog(cm);
            script.invokeFunction("action", 1, 0, 1);
            script.invokeFunction("action", 1, 0, swap ? 1 : 0);
            verify(player, never()).switchAdventurerBranch(anyInt(), anyInt(), anyBoolean(), anyInt(), anyInt());
            verify(cm).sendYesNo(contains(swap ? "力量 300 → 40" : "保留当前全部属性加点"));
            script.invokeFunction("action", 1, 0, 0);
            script.invokeFunction("action", 1, 0, 0);
            verify(player, times(1)).switchAdventurerBranch(520, 510, swap, 300, 40);
        }
    }

    @Test void mageCanCancelOrConfirmWithoutAStatConversionMenu() throws Exception {
        for (boolean confirm : new boolean[]{false, true}) {
            NPCConversationManager cm = mock(NPCConversationManager.class);
            Character player = mock(Character.class);
            when(cm.getPlayer()).thenReturn(player);
            when(cm.getJobId()).thenReturn(220);
            when(cm.getLevel()).thenReturn(40);
            when(player.getStr()).thenReturn(4);
            when(player.getDex()).thenReturn(4);
            when(player.switchAdventurerBranch(230, 220, false, 4, 4)).thenReturn("ok");
            Invocable script = dialog(cm);
            script.invokeFunction("action", 1, 0, 2);
            verify(cm, times(1)).sendSimple(anyString());
            verify(cm).sendYesNo(contains("牧师"));
            script.invokeFunction("action", confirm ? 1 : 0, 0, 0);
            verify(player, times(confirm ? 1 : 0)).switchAdventurerBranch(230, 220, false, 4, 4);
        }
    }
}
