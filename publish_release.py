import paramiko, os

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.168.100.51', username='unclebug', key_filename=os.path.expanduser('~/.ssh/id_ed25519'), timeout=5)

sftp = ssh.open_sftp()
print("Uploading 1_ZubrCraft_Optimization_Base.zip to server...")
sftp.put('1_ZubrCraft_Optimization_Base.zip', '/home/unclebug/storage/minecraft/1_ZubrCraft_Optimization_Base.zip')

print("Uploading ZubrCraft-Prism.zip to server...")
sftp.put('ZubrCraft-Prism.zip', '/home/unclebug/storage/minecraft/ZubrCraft-Prism.zip')
sftp.close()

print("Executing make_release.py on server...")
cmd = "cd /home/unclebug/storage/minecraft && python3 make_release.py v1.0.0"
stdin, stdout, stderr = ssh.exec_command(cmd)
print(stdout.read().decode())
print(stderr.read().decode())

ssh.close()
