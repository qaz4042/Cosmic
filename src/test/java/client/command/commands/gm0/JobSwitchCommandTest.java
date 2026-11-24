package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.Job;
import net.packet.Packet;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class JobSwitchCommandTest {
    @Test void statusQueryDoesNotOpenOrInterruptNpcDialogue() {
        Client client = mock(Client.class, RETURNS_DEEP_STUBS);
        Character player = mock(Character.class, RETURNS_DEEP_STUBS);
        when(client.getPlayer()).thenReturn(player);
        when(player.getJob()).thenReturn(Job.FIGHTER);
        when(player.getLevel()).thenReturn(40);
        new JobSwitchCommand().execute(client, new String[]{"status"});
        verify(player).sendPacket(any(Packet.class));
        verify(client, never()).getCM();
        verify(client, never()).getAbstractPlayerInteraction();
    }
}
