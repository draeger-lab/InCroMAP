@echo off
echo Bilder werden falsch konvertiert. Also einmal konvertieren und
echo images.tex öffnen. dann oben alle algorithmus pakete und html only
echo deklarationen entfernen. Datei schreibgeschützt machen und neu
echo zu HTML konvertieren.
echo.
echo 0.Bildgrößen konvertieren. (B), longSide = 800 bei HTMLReport = 400.
echo 1.Dieses Skript ausführen
echo 2.images.tex anpassen, Schreibschützen, generierte Bilder löschen und skript nochmal ausführen.
echo 3.Stylesheet anpassen (siehe dieses Dokument, REM1)
echo 4.index und mm_doc...html anpassen (siehe REM2, Titel H1 setzen).
echo 5.Alles kopieren, PHP Skript appendLayout.php replaceFiles.bat ausführen.
pause
@cd C:\Dokume~1\wrzodek\Desktop\ModuleMaster_Documentation\
@C:\Tools\latex2html\bin\latex2html.bat -split 3 -info 0 -t ModuleMaster mm_documentation
REM -html_version 4.0+latin1+unicode
pause

goto end
REM
H1		{ font-size : x-large; font-weight: bold;}
    	      
/* document-specific styles come next */
BODY {
  font-family: Helvetica;
  color: #000000;
}
a               {text-decoration: none;}
a:link          {color:#0000FF;
                 text-decoration:none;}
a:visited       {color:#0000FF;
                 text-decoration:none;}
a:focus         {color:#0000FF;}
a:hover         {text-decoration:none; background-color: #EEEEEE;}
a:active        {color:#0000FF;}

...AND MORE. Copy File!

REM 2x !!


		<H1 ALIGN=CENTER><TABLE CELLPADDING=3>
<TR><TD ALIGN="LEFT" VALIGN="TOP" WIDTH="100%">
<DIV ALIGN="CENTER"><H1>Module Master: a new tool to decipher transcriptional regulatory networks</H1></TD>
</TR>
</TABLE></H1>



:end
exit
