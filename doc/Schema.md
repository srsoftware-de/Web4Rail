# grundlegende Funktionsweise

## Software-Ebenen

Bei der digitalen Modellbahn gibt es typischerweise drei Software-Ebenen:

1. Signal-Erzeugung:  
Das Digital-Signal, welches durch einen Booster verstärkt an die Lokomotiven und Zubehör-Artikel geleitet wird, muss erzeugt werden.
Das kann durch eine speziell dafür ausgelegte Hardware(-Zentrale) erfolgen, oder eben durch ein Programm wie [SRCPD].  
  
2. Funktionale Zentrale:  
Diese Software-Schicht kümmert sich um die logische Steuerung. Das bedeutet, Programme in dieser Schicht verfolgen den Zustand von Modellbahnfahrzeugen (Position, Geschwindikeit, Status der Funktionen) und Zubehörartikeln (Weichen, Relais, Signale) und verwalten die Gleisanlage (Besetztmeldungen, Gleissperren, etc.).  
  
3. Nutzer-Interaktionsschicht  
Diese Software-Ebene stellt den Zustand der Modellbahn für den Nutzer dar und erlaubt dem Nutzer, mit der Modellbahn zu interagieren.

[Schema](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/srsoftware-de/Web4Rail/master/doc/schema.plantuml)

## Web4Rail

Web4Rail als Software stellt nun die Ebene 2 dieses Software-Stapels bereit:  
Dazu werden in Web4Rail Züge und der Gleisplan gespeichert, sowie die Status aller Fahrzeuge, Zubehörartikel und anderen Teile einer Modellbahnanalage.

Immer wenn der Nutzer irgendwelche Aktionen in Web4Rail veranlasst, erzeugt dieses die Steuerbefehle, die an die Ebene-1-Software ([SRCPD]) gesendet werden.
Außerdem nimmt es natürlich auch Informationen (z.B. Rückmeldungen) von der Ebene-1-Software an und verarbeitet diese.

Daneben erzeugt es die Darstellungsinformationen für die Ebene-3-Software: es erzeugt quasi eine Website, so dass der Nutzer über einen beliebigen Webbrowser seine Modellbahn steuern kann.


[SRCPD]: http://srcpd.sourceforge.net/srcpd/