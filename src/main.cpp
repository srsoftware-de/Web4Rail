#include <iostream>
#include <unistd.h>

#ifdef linux
#include <netdb.h> // gethostbyname(), hostent
#include <arpa/inet.h> // inet_ntoa()
#else
#include <winsock2.h>
#endif

int main(){
	using namespace std;

#ifndef linux
	WSADATA w;
	if(int result = WSAStartup(MAKEWORD(2,2), &w) != 0){
		cout << "Winsock 2 konnte nicht gestartet werden! Error #" << result << endl;
		return 1;
	}
#endif

	cout << "Bitte gebe einen Hostnamen ein: ";
	string hostname;
	cin >> hostname;

	hostent *phe = gethostbyname(hostname.c_str());

	if(phe == NULL){
		cout << "Host konnte nicht aufgeloest werden!" << endl;
		return 1;
	}

	cout << "\nHostname: " << phe->h_name << endl << "Aliases: ";

	for(char** p = phe->h_aliases; *p != 0; ++p) cout << *p << " ";
	cout << endl;

	if(phe->h_addrtype != AF_INET){
		cout << "Ungueltiger Adresstyp!" << endl;
		return 1;
	}

	if(phe->h_length != 4){
		cout << "Ungueltiger IP-Typ!" << endl;
		return 1;
	}

	int Socket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if(Socket == -1){
		cout << "Socket konnte nicht erstellt werden!" << endl;
		return 1;
	}

	sockaddr_in service;
	service.sin_family = AF_INET;
	service.sin_port = htons(80); // Das HTTP-Protokoll benutzt Port 80

	char** p = phe->h_addr_list; // p mit erstem Listenelement initialisieren
	int result; // Ergebnis von connect
	do {
		if(*p == NULL) {
			cout << "Verbindung fehlgschlagen!" << endl;
			return 1;
		}

		service.sin_addr.s_addr = *reinterpret_cast<unsigned long*>(*p);
		p++;
		result = connect(Socket, reinterpret_cast<sockaddr*>(&service), sizeof(service));
	}
	while(result == -1);

	cout << "Verbindung erfolgreich!" << endl;

#ifdef linux
	close(Socket);
#else
	closesocket(Socket);
#endif
}
