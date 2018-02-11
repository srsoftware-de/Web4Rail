#include <iostream>
#include <fstream>
#include <stdexcept> // runtime_error
#include <sstream>
#include <cstring>
#include <unistd.h>

#ifdef linux
#include <sys/socket.h> // socket(), connect()
#include <arpa/inet.h> // sockaddr_in
#include <netdb.h> // gethostbyname(), hostent
#include <errno.h> // errno
#else
#include <winsock2.h>
#endif

// erzeugt eine Exception, verwendet dabei die globale Variable errno
std::runtime_error CreateSocketError(){
	std::ostringstream temp;
#ifdef linux
	temp << "Socket-Fehler #" << errno << ": " << strerror(errno);
#else
	int error = WSAGetLastError();
	temp << "Socket-Fehler #" << error;
	char* msg;
	if(FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,NULL, error, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),reinterpret_cast<char*>(&msg), 0, NULL)){
		try{
			temp << ": " << msg;
			LocalFree(msg);
		}catch(...){
			LocalFree(msg);
			throw;
		}
	}
#endif
	return std::runtime_error(temp.str());
}

void SendAll(int socket, const char* const buf, const int size){
	int bytesSent = 0; // Anzahl Bytes die wir bereits vom Buffer gesendet haben
	do{
		int result = send(socket, buf + bytesSent, size - bytesSent, 0);
		if(result < 0) throw CreateSocketError();
		bytesSent += result;
	} while(bytesSent < size);
}

// Liest eine Zeile des Sockets in einen stringstream
void GetLine(int socket, std::stringstream &line){
	for(char c; recv(socket, &c, 1, 0) > 0; line << c){
		if(c == '\n') return;
	}
	throw CreateSocketError();
}

// Entfernt das http:// vor dem URL
void RemoveHttp(std::string& url){
	size_t pos = url.find("http://");
	if(pos != std::string::npos) url.erase(0, 7);
}

// Gibt die Dateiendung im URL zurück
std::string GetFileEnding(std::string& URL){
	using namespace std;
	size_t pos = URL.rfind(".");
	if(pos == string::npos)return "";
	URL.erase(0, pos);
	string ending = ".";
	// Algorithmus um Sachen wie ?index=home nicht zuzulassen
	for(string::iterator it = URL.begin() + 1; it != URL.end(); ++it){
		if(isalpha(*it)){
			ending += *it;
		}else break;
	}
	return ending;
}

// Gibt den Hostnamen zurück und entfernt ihn aus der URL, sodass nur noch der Pfad übrigbleibt
std::string RemoveHostname(std::string& URL){
	size_t pos = URL.find("/");
	if(pos == std::string::npos){
		std::string temp = URL;
		URL = "/";
		return temp;
	}
	std::string temp = URL.substr(0, pos);
	URL.erase(0, pos);
	return temp;
}

int main(){
	using namespace std;

	cout << "URL: ";
	string url;
	cin >> url; // User gibt URL der Datei ein, die herruntergeladen werden soll

#ifndef linux
	WSADATA w;
	if(int result = WSAStartup(MAKEWORD(2,2), &w) != 0){
		cout << "Winsock 2 konnte nicht gestartet werden! Error #" << result << endl;
		return 1;
	}
#endif

	RemoveHttp(url);

	string hostname = RemoveHostname(url);

	hostent* phe = gethostbyname(hostname.c_str());

	if(phe == NULL){
		cout << "Host konnte nicht aufgelöst werden!" << endl;
		return 1;
	}

	if(phe->h_addrtype != AF_INET){
		cout << "Ungültiger Adresstyp!" << endl;
		return 1;
	}

	if(phe->h_length != 4){
		cout << "Ungültiger IP-Typ!" << endl;
		return 1;
	}

	int Socket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
	if(Socket == -1){
		cout << "Socket konnte nicht erstellt werden!" << endl;
		return 1;
	}

	sockaddr_in service;
	service.sin_family = AF_INET;
	service.sin_port = htons(80); // Das HTTP-Protokoll benutzt Port 80

	char** p = phe->h_addr_list; // p mit erstem Listenelement initialisieren
	int result; // Ergebnis von connect
	do{
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

	string request = "GET ";
	request += url;	// z.B. /faq/index.html
	request += " HTTP/1.1\n";
	request += "Host: " + hostname + "\nConnection: close\n\n";

	try{
		SendAll(Socket, request.c_str(), request.size());

		int code;
		string Protokoll;
		stringstream firstLine; // Die erste Linie ist anders aufgebaut als der Rest
		do{
			GetLine(Socket, firstLine);
			firstLine >> Protokoll;
			firstLine >> code;
			// 100 = Continue
			if(code == 100) GetLine(Socket, firstLine); // Leere Zeile nach Continue ignorieren
		} while(code == 100);
		cout << "Protokoll: " << Protokoll << endl;

		if(code != 200){
			firstLine.ignore(); // Leerzeichen nach dem Statuscode ignorieren
			string msg;
			getline(firstLine, msg);
			cout << "Error #" << code << " - " << msg << endl;
			return 0;
		}

		bool chunked = false;
		const int noSizeGiven = -1;
		int size = noSizeGiven;

		while(true){
			stringstream sstream;
			GetLine(Socket, sstream);
			if(sstream.str() == "\r") break;// Header zu ende?

			string left; // Das was links steht
			sstream >> left;
			sstream.ignore(); // ignoriert Leerzeichen
			if(left == "Content-Length:") sstream >> size;
			if(left == "Transfer-Encoding:"){
				string transferEncoding;
				sstream >> transferEncoding;
				chunked = (transferEncoding == "chunked");
			}
		}

		string filename = "download" + GetFileEnding(url);
		cout << "Filename: " << filename << endl;
		fstream fout(filename.c_str(), ios::binary | ios::out);
		if(!fout){
			cout << "Could not create file!" << endl;
			return 1;
		}
		int recvSize = 0; // Empfangene Bytes insgesamt
		char buf[1024];
		int bytesRecv = -1; // Empfangene Bytes des letzten recv

		if(size != noSizeGiven){ // Wenn die Größe über Content-length gegeben wurde
			cout << "0%";
			while(recvSize < size){
				if((bytesRecv = recv(Socket, buf, sizeof(buf), 0)) <= 0) throw CreateSocketError();
				recvSize += bytesRecv;
				fout.write(buf, bytesRecv);
				cout << "\r" << recvSize * 100 / size << "%" << flush; // Mit \r springen wir an den Anfang der Zeile
			}
		}else{
			if(!chunked){
				cout << "Downloading... (Unknown Filesize)" << endl;
				while(bytesRecv != 0){ // Wenn recv 0 zurück gibt, wurde die Verbindung beendet
					if((bytesRecv = recv(Socket, buf, sizeof(buf), 0)) < 0) throw CreateSocketError();
					fout.write(buf, bytesRecv);
				}
			}else{
				cout << "Downloading... (Chunked)" << endl;
				while(true){
					stringstream sstream;
					GetLine(Socket, sstream);
					int chunkSize = -1;
					sstream >> hex >> chunkSize; // Größe des nächsten Parts einlesen
					if(chunkSize <= 0) break;
					cout << "Downloading Part (" << chunkSize << " Bytes)... " << endl;
					recvSize = 0; // Vor jeder Schleife wieder auf 0 setzen
					while(recvSize < chunkSize){
						int bytesToRecv = chunkSize - recvSize;
						if((bytesRecv = recv(Socket, buf, bytesToRecv > sizeof(buf) ? sizeof(buf) : bytesToRecv, 0)) <= 0) throw CreateSocketError();
						recvSize += bytesRecv;
						fout.write(buf, bytesRecv);
						cout << "\r" << recvSize * 100 / chunkSize << "%" << flush;
					}
					cout << endl;
					for(int i = 0; i < 2; ++i){
						char temp;
						recv(Socket, &temp, 1, 0);
					}
				}
			}
		}
		cout << endl << "Finished!" << endl;
	} catch(exception& e){
		cout << endl;
		cerr << e.what() << endl;
	}
#ifdef linux
	close(Socket); // Verbindung beenden
#else
	closesocket(Socket); // Windows-Variante
#endif
}
