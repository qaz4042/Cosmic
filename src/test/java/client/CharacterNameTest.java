package client;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import tools.StringUtil;
import static org.junit.jupiter.api.Assertions.*;
class CharacterNameTest {
    @Test void acceptsChineseWithinProtocolLimit() {
        for (String name : new String[]{"abc", "123", "ABC123xyz789", "卡卡寂", "卡卡寂312", "中文名字"})
            assertTrue(CharacterName.isValid(name), name);
        for (String name : new String[]{"", "ab", "卡卡", "中文名字啊", "卡卡寂1234", "abc def", "abc_123", "ＡＢＣ", "abc\n", "😀abc"})
            assertFalse(CharacterName.isValid(name), name);
    }
    @Test void fixedNamesHaveExactlyThirteenBytes() {
        for (String name : new String[]{"abc", "卡卡寂", "卡卡寂312", "中文名字", "中文名字太长"}) {
            byte[] bytes = StringUtil.getRightBytePaddedStr(name, 13).getBytes(StandardCharsets.UTF_8);
            assertEquals(13, bytes.length);
            assertEquals(0, bytes[12]);
            assertFalse(new String(bytes, StandardCharsets.UTF_8).contains("\uFFFD"));
        }
    }
}
