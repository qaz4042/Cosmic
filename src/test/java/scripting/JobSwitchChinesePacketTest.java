package scripting;

import constants.string.CharsetConstants;
import net.packet.ByteBufOutPacket;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class JobSwitchChinesePacketTest {
    @Test void chineseDialogueSurvivesActualPacketWriter() {
        assertEquals(StandardCharsets.UTF_8, CharsetConstants.CHARSET);
        String text = "职业切换：自动卸下全部装备\r\n请选择目标分支：牧师";
        ByteBufOutPacket packet = new ByteBufOutPacket();
        packet.writeString(text);
        byte[] bytes = packet.getBytes();
        assertEquals(text.getBytes(StandardCharsets.UTF_8).length, (bytes[0]&255) | ((bytes[1]&255)<<8));
        assertEquals(text, new String(bytes, 2, bytes.length-2, StandardCharsets.UTF_8));
    }
}
