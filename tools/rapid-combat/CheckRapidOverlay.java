import client.Character;
import client.Client;
import client.command.commands.gm0.AssistCommand;
import config.YamlConfig;
import net.packet.Packet;
import server.TrainingRapidCombat;
import server.maps.MapleMap;
import static org.mockito.Mockito.*;

/** Exercises the packaged deployed overlays, rather than the unextended main Character. */
public class CheckRapidOverlay {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        YamlConfig.config.server.TRAINING_ASSIST_MAPS="*";
        Character p=mock(Character.class,CALLS_REAL_METHODS);
        MapleMap map=mock(MapleMap.class);
        doReturn(100000000).when(map).getId();
        doReturn(java.util.List.of()).when(map).getAllMonsters();
        doReturn(map).when(p).getMap(); doReturn(100000000).when(p).getMapId();
        doReturn(true).when(p).isAlive(); doReturn(true).when(p).isLoggedin();
        doReturn(false).when(p).isChangingMaps();
        doReturn(1).when(p).getSkillLevel(5001001);
        doReturn(1).when(p).getSkillLevel(5001003);
        doNothing().when(p).yellowMessage(anyString());
        java.util.List<String> notices=new java.util.ArrayList<>();
        doAnswer(a->{ notices.add(new String(((Packet)a.getArgument(0)).getBytes(),java.nio.charset.StandardCharsets.UTF_8)); return null; })
                .when(p).sendPacket(any(Packet.class));
        Client client=mock(Client.class); when(client.getPlayer()).thenReturn(p);
        AssistCommand command=new AssistCommand();
        for (int rate : new int[]{10,20,40}) {
            command.execute(client,new String[]{"rapid","on",Integer.toString(rate),"5001003"});
            check(TrainingRapidCombat.active(p)); check(p.getTrainingAutoSkill()==5001003);
            check(notices.stream().anyMatch(n->n.contains("[TrainingAssist:rapid:v1] 100000000 5001003 "+rate+" ")));
        }
        command.execute(client,new String[]{"rapid","on","60","5001003"});
        check(TrainingRapidCombat.active(p)); // invalid update preserves the running configuration
        command.execute(client,new String[]{"rapid","off"});
        check(!TrainingRapidCombat.active(p) && p.getTrainingAutoSkill()==-1);
        command.execute(client,new String[]{"rapid","on","20","5001001"});
        check(TrainingRapidCombat.active(p));
        command.execute(client,new String[]{"auto","off"});
        check(!TrainingRapidCombat.active(p) && p.getTrainingAutoSkill()==-1);
        command.execute(client,new String[]{"rapid","on","20","5001001"});
        command.execute(client,new String[]{"auto","on","5001003"});
        check(!TrainingRapidCombat.active(p) && p.getTrainingAutoSkill()==5001003);
        command.execute(client,new String[]{"rapid","on","20","5001001"});
        command.execute(client,new String[]{"off"});
        check(!TrainingRapidCombat.active(p) && p.getTrainingAutoSkill()==-1);
        YamlConfig.config.server.TRAINING_ASSIST_MAPS="";
        command.execute(client,new String[]{"rapid","on","20","5001001"});
        check(!TrainingRapidCombat.active(p));
        check(notices.stream().anyMatch(n->n.contains("[TrainingAssist:petvac:v2]")));
        System.out.println("PASS: packaged assist commands, 10/20/40 acknowledgements, invalid updates, normal-auto switch, all stop paths, map policy and pet-v2 compatibility");
    }
}
