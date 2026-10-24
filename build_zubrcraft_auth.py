import paramiko, os, zipfile

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.168.100.51', username='unclebug', key_filename=os.path.expanduser('~/.ssh/id_ed25519'))

# 1. Clear /tmp/zubrcraft_auth
ssh.exec_command('rm -rf /tmp/zubrcraft_auth && mkdir -p /tmp/zubrcraft_auth/src /tmp/zubrcraft_auth/bin /tmp/zubrcraft_auth/res')

# 2. Upload source files
sftp = ssh.open_sftp()

local_root = os.path.abspath('zubrcraft_auth')
for root, dirs, files in os.walk(local_root):
    for f in files:
        full_path = os.path.join(root, f)
        rel_path = os.path.relpath(full_path, local_root).replace('\\', '/')
        dest_path = f'/tmp/zubrcraft_auth/{rel_path}'
        
        # Ensure parent dir exists
        parent = os.path.dirname(dest_path)
        try:
            stdin, stdout, stderr = ssh.exec_command(f'mkdir -p "{parent}"')
            stdout.channel.recv_exit_status()
        except:
            pass
        
        print(f"Uploading {rel_path} -> {dest_path}")
        sftp.put(full_path, dest_path)

# 3. Compile sources on server
compile_cmd = """
CP="$(cat /tmp/mc_classpath.txt):/tmp/client-1.20.1-srg.jar"
find /tmp/zubrcraft_auth/src -name "*.java" > /tmp/zubrcraft_auth/sources.txt
echo "Sources to compile:"
cat /tmp/zubrcraft_auth/sources.txt

javac -encoding UTF-8 -cp "$CP" @/tmp/zubrcraft_auth/sources.txt -d /tmp/zubrcraft_auth/bin
"""

stdin, stdout, stderr = ssh.exec_command(compile_cmd)
exit_status = stdout.channel.recv_exit_status()
out = stdout.read().decode('utf-8', 'replace')
err = stderr.read().decode('utf-8', 'replace')

print("COMPILE STATUS:", exit_status)
print("STDOUT:\n", out)
print("STDERR:\n", err)

if exit_status != 0:
    print("Compilation failed! Exiting.")
    ssh.close()
    exit(1)

# 4. Package JAR
pack_cmd = """
cd /tmp/zubrcraft_auth/bin
cp -r /tmp/zubrcraft_auth/resources/* .
jar cf /tmp/zubrcraft_auth-1.20.1-1.0.0.jar .
ls -la /tmp/zubrcraft_auth-1.20.1-1.0.0.jar
"""

stdin, stdout, stderr = ssh.exec_command(pack_cmd)
stdout.channel.recv_exit_status()
print(stdout.read().decode())

# 5. Download built JAR
local_jar = 'zubrcraft_auth-1.20.1-1.0.0.jar'
sftp.get('/tmp/zubrcraft_auth-1.20.1-1.0.0.jar', local_jar)
print(f"Downloaded built mod to {local_jar} ({os.path.getsize(local_jar)} bytes)")

sftp.close()
ssh.close()
