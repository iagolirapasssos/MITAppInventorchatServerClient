from cryptography.fernet import Fernet
import socket
import threading
import os

# Gere uma chave para esta sessão do servidor (isso deve ser feito de forma mais segura na prática)
key = Fernet.generate_key()
cipher_suite = Fernet(key)

# Configurações do servidor
host = '0.0.0.0'  
port = 12345      

clients = []

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind((host, port))
server.listen()

def broadcast(message, _sender=None):
    for client in clients:
        if client is not _sender:
            try:
                client.send(message)
            except:
                client.close()
                remove_client(client)

def handle_client(client):
    while True:
        try:
            encrypted_message = client.recv(1024)
            message = cipher_suite.decrypt(encrypted_message).decode()
            broadcast(cipher_suite.encrypt(message.encode()), client)
        except:
            remove_client(client)
            break

def remove_client(client):
    if client in clients:
        clients.remove(client)
        client.close()

def main():
    print(f"Servidor escutando em {host}:{port}")

    while True:
        client, address = server.accept()
        print(f"Conexão estabelecida com {address}")

        # Envie a chave de criptografia para o cliente
        client.send(key)

        clients.append(client)
        thread = threading.Thread(target=handle_client, args=(client,))
        thread.start()

if __name__ == "__main__":
    main()
