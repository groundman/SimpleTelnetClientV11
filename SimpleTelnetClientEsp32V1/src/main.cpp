//codigo compatible con la App de andorid SimpleTelnetClientV11
#include <Arduino.h>
#include "WiFi.h"


#define MAX_SRV_CLIENTS 1
WiFiServer server(23);
WiFiClient serverClients[MAX_SRV_CLIENTS];

String inCharHex="";
String tramaInHexString="";
String inByteHexString="";
byte   inByte[1024];
int16_t Cont=0;


boolean conectado=false;
boolean trama_in=false; 


void sendString(String tramaInHexString){
    for (int ntm2 =0; ntm2 < tramaInHexString.length(); ++ntm2){
      serverClients[0].write(tramaInHexString[ntm2]);
    }
    tramaInHexString="";                                                        //borra los datos recibidos
}



void respuestaHost(){
  serverClients[0].write("Eco ");
  sendString(tramaInHexString);
  trama_in=false;
}




void sendHost(){                                                                //envio de datos al host
  Serial.println("sendHost");
  

    Cont++;
    
  String strCount=String(Cont);
 
  char chrSend[strCount.length()];
  strCount.toCharArray(chrSend, strCount.length());
  serverClients[0].write("Trama Entrante Nº");

    for (int ntm =0; ntm < strCount.length(); ++ntm){
      serverClients[0].write(strCount[ntm]);
    }

    serverClients[0].write('\n');
    

   delayMicroseconds(100000);

}




void setup() {
   Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }

   WiFi.softAP("Solda Celulas", "12345678");
     delay(4000);
      server.begin();
      Serial.println("server started");

      Serial.println("IP_AP");
      Serial.println(WiFi.softAPIP());
}



void loop() {
  // put your main code here, to run repeatedly:
  Serial.println("loop "+String(conectado));
  delayMicroseconds(100000);

   if(trama_in)  respuestaHost();                                           //los datos recividos del cliente son reenviados
   if(conectado)     sendHost();





//******************************************************************************** GESTIONN WI-FI ****************************************************************************
        uint8_t i;
        
        String StrReadBytes;
        String byteHex;
        String palabraByteHex;


      //comprueba si hay nuevos clientes
      //Serial.println(server.hasClient());
      if (server.hasClient()){
        for(i = 0; i < MAX_SRV_CLIENTS; i++){
          if (!serverClients[i] || !serverClients[i].connected()){
            if(serverClients[i]) serverClients[i].stop();
            serverClients[i] = server.available();
            Serial.println("Nuevo cliente: ");
            Serial.print(i);
            conectado=true;
            continue;
          }
        }
        
        WiFiClient serverClient = server.available();
        serverClient.stop();
        Serial.println("Client Stop");
        Serial.println(i);
      }


      //check clients for data
      uint8_t w=0;
      //uint8_t out=0;
      inByteHexString="";
      inCharHex="";
      tramaInHexString="";
      
      

      for(i = 0; i < MAX_SRV_CLIENTS; i++){
        if (serverClients[i] && serverClients[i].connected()){
          if(serverClients[i].available()){
              //N_tramas++;
              trama_in=true;
            //gestiona los datos recibidos por wi-fi
            while(serverClients[i].available()) {                             //bucle mientras haya datos en el buffer

                inByte[w] = (byte)serverClients[i].read();                      //coje el dato del buffer en formato byte
                inCharHex += (char)inByte[w];                                   //transforma el dato recibido en formato CHAR.
                //Serial.println(String((char)inByte[w])+" index "+String(w));
                inByteHexString=String(inByte[w],HEX);                          //crea un string que contenga la trama(para visualizacion)
                if (inByteHexString.length()<2) inByteHexString= "0" + inByteHexString ;  //añade un cero delante de cifras simples
                tramaInHexString += inByteHexString +" ";                       //trama de valores hexdecimales(para visualizacion)
                //Serial.println("permiso trama "+String(permisoTrama)+" "+String(inByte[w]));
                w++;

              }
              while(serverClients[i].available()){
                serverClients[i].read();                                        //limpiar buffer de recepcion
                Serial.println("borradoooooo");
              }

              Serial.println(inCharHex+"--->"+tramaInHexString);
              Serial.println("cantidad bytes leidos wi-fi "+String(w));
              w=0;
              inByteHexString="";
              //tramaInHexString="";
              memset(inByte,0,sizeof(inByte));                                    //borra la matriz de arrays

          }
        }
      
      }
}