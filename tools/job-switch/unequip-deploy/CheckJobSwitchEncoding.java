import constants.string.CharsetConstants;
import net.packet.ByteBufOutPacket;
import java.nio.charset.StandardCharsets;
public class CheckJobSwitchEncoding {
 public static void main(String[] args) {
  if (!CharsetConstants.CHARSET.equals(StandardCharsets.UTF_8)) throw new AssertionError("Not UTF-8");
  String text="职业切换：自动卸下全部装备，牧师";
  ByteBufOutPacket p=new ByteBufOutPacket();p.writeString(text);
  byte[] b=p.getBytes();
  if (!text.equals(new String(b,2,b.length-2,StandardCharsets.UTF_8))) throw new AssertionError("Chinese changed");
  System.out.println("PASS: production packet writer preserves Chinese using UTF-8 configuration");
 }
}
