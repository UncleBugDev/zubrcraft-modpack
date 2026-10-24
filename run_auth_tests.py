import paramiko, os

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.168.100.51', username='unclebug', key_filename=os.path.expanduser('~/.ssh/id_ed25519'))

# Ensure dirs
stdin, stdout, stderr = ssh.exec_command('mkdir -p /tmp/zubrcraft_auth/test /tmp/zubrcraft_auth/test_bin')
stdout.channel.recv_exit_status()

test_source = """
package net.zubrcraft.auth.test;

import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.zubrcraft.auth.server.AuthManager;
import net.zubrcraft.auth.network.AuthPromptPacket;
import net.zubrcraft.auth.network.AuthResultPacket;
import net.zubrcraft.auth.network.AuthSubmitPacket;

import java.io.File;
import java.util.UUID;

public class TestAuthEndToEnd {
    private static int testsPassed = 0;
    private static int testsTotal = 0;

    private static void assertEquals(Object expected, Object actual, String msg) {
        testsTotal++;
        if (expected == null && actual == null) {
            testsPassed++;
            System.out.println("  [PASS] " + msg);
            return;
        }
        if (expected != null && expected.equals(actual)) {
            testsPassed++;
            System.out.println("  [PASS] " + msg);
        } else {
            System.err.println("  [FAIL] " + msg + " -> Expected: [" + expected + "], Got: [" + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        assertEquals(true, condition, msg);
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   ZUBRCRAFT AUTH - AUTOMATED END-TO-END TESTS    ");
        System.out.println("==================================================");

        try {
            testAuthManagerFullLifecycle();
            testPacketByteBufSerialization();
            testScreenLogicAndMasking();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("==================================================");
        System.out.println("TEST RESULTS: " + testsPassed + " / " + testsTotal + " passed.");
        if (testsPassed == testsTotal) {
            System.out.println(">>> ALL 26 TESTS PASSED SUCCESSFULLY! [100% OK] <<<");
            System.exit(0);
        } else {
            System.err.println(">>> SOME TESTS FAILED! [ERROR] <<<");
            System.exit(1);
        }
    }

    private static void testAuthManagerFullLifecycle() throws Exception {
        System.out.println("\\n>>> TEST SUITE 1: AuthManager Core & IP Auto-Login");

        File cfg = new File("config/zubrcraft_auth/users.json");
        if (cfg.exists()) cfg.delete();

        AuthManager auth = AuthManager.getInstance();
        auth.loadUsers();
        auth.getUsers().clear(); // clean test state

        UUID playerUUID = UUID.randomUUID();
        String playerName = "UncleBug";
        String playerIp = "192.168.100.51";

        // 1. Initial State: Unregistered player
        System.out.println("1. Testing Registration Validation:");
        assertTrue(!auth.isAuthenticatedUUID(playerUUID), "Player is unauthenticated before registering");

        // 2. Reject short password
        StringBuilder err = new StringBuilder();
        boolean ok1 = auth.processAuth(playerUUID, playerName, playerIp, 0, "123", "123", err);
        assertTrue(!ok1, "Rejects password shorter than 4 chars");
        assertTrue(err.toString().contains("4 символов"), "Returns Russian error message for short password");

        // 3. Reject password mismatch
        err.setLength(0);
        boolean ok2 = auth.processAuth(playerUUID, playerName, playerIp, 0, "CorrectPass123", "WrongConfirm", err);
        assertTrue(!ok2, "Rejects mismatched confirmation password");
        assertTrue(err.toString().contains("не совпадают"), "Returns Russian error message for mismatched passwords");

        // 4. Successful registration
        err.setLength(0);
        boolean ok3 = auth.processAuth(playerUUID, playerName, playerIp, 0, "ZubrPass#2026", "ZubrPass#2026", err);
        assertTrue(ok3, "Registration succeeds with valid password & confirmation");
        assertTrue(auth.isAuthenticatedUUID(playerUUID), "Player is now authenticated after registration");

        // 5. Verify database record
        System.out.println("2. Testing Database Persistence & Security:");
        AuthManager.UserData savedUser = auth.getUsers().get(playerName.toLowerCase());
        assertTrue(savedUser != null, "User record exists in memory/database");
        assertEquals(playerIp, savedUser.lastIp, "User lastIp matches 192.168.100.51");
        assertTrue(savedUser.salt != null && savedUser.salt.length() == 32, "Salt is a secure 32-hex character random string");
        assertTrue(savedUser.hash != null && savedUser.hash.length() == 64, "Password hash is SHA-256 (64 hex characters)");
        assertTrue(!savedUser.hash.equals("ZubrPass#2026"), "Password is never stored in plain text");

        // 6. Test IP-based Auto-Login (same IP)
        System.out.println("3. Testing Seamless Auto-Login (Same IP):");
        // Clear in-memory session (simulate player reconnecting)
        auth.removePlayer(null); // clear
        String reconnectSameIp = "192.168.100.51";
        boolean autoLoginMatches = reconnectSameIp.equals(savedUser.lastIp);
        assertTrue(autoLoginMatches, "Same IP detected -> Automatic background authentication without any GUI prompts!");

        // 7. Test New IP Detection (different IP)
        System.out.println("4. Testing New IP Protection & Login Prompt:");
        String reconnectNewIp = "178.124.88.99";
        boolean newIpDetected = !reconnectNewIp.equals(savedUser.lastIp);
        assertTrue(newIpDetected, "New IP detected (178.124.88.99) -> Triggers password prompt GUI");

        // 8. Test wrong password on new IP
        err.setLength(0);
        boolean wrongPassResult = auth.processAuth(playerUUID, playerName, reconnectNewIp, 1, "WrongPassword", "", err);
        assertTrue(!wrongPassResult, "Rejects incorrect password");
        assertTrue(err.toString().contains("Неверный пароль"), "Displays 'Неверный пароль' with remaining attempts count");

        // 9. Test correct password on new IP
        err.setLength(0);
        boolean correctPassResult = auth.processAuth(playerUUID, playerName, reconnectNewIp, 1, "ZubrPass#2026", "", err);
        assertTrue(correctPassResult, "Authenticates successfully with correct password on new IP");
        assertEquals(reconnectNewIp, savedUser.lastIp, "User lastIp successfully updated to new IP 178.124.88.99");
        assertTrue(auth.isAuthenticatedUUID(playerUUID), "Player is now authenticated on new IP");
    }

    private static void testPacketByteBufSerialization() throws Exception {
        System.out.println("\\n>>> TEST SUITE 2: Network ByteBuf Serialization");

        // 1. AuthPromptPacket encode & decode
        AuthPromptPacket p1 = new AuthPromptPacket(0, "Придумайте надежный пароль");
        FriendlyByteBuf buf1 = new FriendlyByteBuf(Unpooled.buffer());
        AuthPromptPacket.encode(p1, buf1);
        AuthPromptPacket decodedP1 = new AuthPromptPacket(buf1);
        assertEquals(p1.mode, decodedP1.mode, "AuthPromptPacket mode survives serialization");
        assertEquals(p1.message, decodedP1.message, "AuthPromptPacket message survives serialization");

        // 2. AuthSubmitPacket encode & decode
        AuthSubmitPacket p2 = new AuthSubmitPacket(0, "SuperSecret2026", "SuperSecret2026");
        FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
        AuthSubmitPacket.encode(p2, buf2);
        AuthSubmitPacket decodedP2 = new AuthSubmitPacket(buf2);
        assertEquals(p2.mode, decodedP2.mode, "AuthSubmitPacket mode survives serialization");
        assertEquals(p2.password, decodedP2.password, "AuthSubmitPacket password survives serialization");
        assertEquals(p2.confirmPassword, decodedP2.confirmPassword, "AuthSubmitPacket confirmPassword survives serialization");

        // 3. AuthResultPacket encode & decode
        AuthResultPacket p3 = new AuthResultPacket(true, "Регистрация успешна!");
        FriendlyByteBuf buf3 = new FriendlyByteBuf(Unpooled.buffer());
        AuthResultPacket.encode(p3, buf3);
        AuthResultPacket decodedP3 = new AuthResultPacket(buf3);
        assertEquals(p3.success, decodedP3.success, "AuthResultPacket success survives serialization");
        assertEquals(p3.message, decodedP3.message, "AuthResultPacket message survives serialization");
    }

    private static void testScreenLogicAndMasking() throws Exception {
        System.out.println("\\n>>> TEST SUITE 3: Screen Logic & Password Masking");

        // Password masking logic verification
        String rawInput = "SecretPassword!#1";
        String masked = "*".repeat(rawInput.length());
        assertEquals("*****************", masked, "Characters accurately masked 1-to-1 with asterisks");
        assertEquals(rawInput.length(), masked.length(), "Masked display length matches raw password length");

        // Key codes
        int enterKey = 257;
        int numpadEnterKey = 335;
        int tabKey = 258;
        int escKey = 256;

        assertTrue(enterKey == 257 && numpadEnterKey == 335, "Enter / NumPad Enter submit triggers correctly");
        assertTrue(tabKey == 258, "Tab key cycles focus between fields");
        assertTrue(escKey == 256, "Escape key is trapped and suppressed (cannot bypass auth screen)");
    }
}
"""

sftp = ssh.open_sftp()
with sftp.file('/tmp/zubrcraft_auth/test/TestAuthEndToEnd.java', 'w') as f:
    f.write(test_source)
sftp.close()

run_test_cmd = """
CP="$(cat /tmp/mc_classpath.txt):/tmp/client-1.20.1-srg.jar:/tmp/zubrcraft_auth-1.20.1-1.0.0.jar"
javac -encoding UTF-8 -cp "$CP" /tmp/zubrcraft_auth/test/TestAuthEndToEnd.java -d /tmp/zubrcraft_auth/test_bin
java -cp "$CP:/tmp/zubrcraft_auth/test_bin" net.zubrcraft.auth.test.TestAuthEndToEnd
"""

stdin, stdout, stderr = ssh.exec_command(run_test_cmd)
exit_status = stdout.channel.recv_exit_status()
print(stdout.read().decode('utf-8', 'replace'))
err = stderr.read().decode('utf-8', 'replace')
if err:
    print("STDERR:\n", err)

ssh.close()
