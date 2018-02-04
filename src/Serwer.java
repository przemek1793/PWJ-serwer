import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Properties;

public class Serwer implements Runnable
{
    Socket csocket;
    static String AdresBazyDanych="192.168.0.13";
    static String NazwaBazyDanych="PWJ_Projekt";
    static String NazwaUzytkownika="PWJ";
    static String HasłoDoBazy="asdf";
    String TypZalogowanego="";
    String ObecnieZalogowany="";
    Serwer(Socket csocket)
    {
        this.csocket = csocket;
    }

    static boolean ladujSterownik()
    {
        // LADOWANIE STEROWNIKA
        System.out.print("Sprawdzanie sterownika:");
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            return true;
        } catch (Exception e) {
            System.out.println("Blad przy ladowaniu sterownika bazy!");
            return false;
        }
    }

    /**
     * Metoda s�u�y do po��czenia z MySQL bez wybierania konkretnej bazy
     *
     * @return referencja do uchwytu bazy danych
     */
    private static Connection getConnection(String adres, int port) {

        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", NazwaUzytkownika);
        connectionProps.put("password", HasłoDoBazy);

        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + adres + ":" + port + "/",
                    connectionProps);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * Metoda s�u�y do nawi�zania po��czenia z baz� danych
     *
     * @param adress
     *            - adres bazy danych
     * @param dataBaseName
     *            - nazwa bazy
     * @param userName
     *            - login do bazy
     * @param password
     *            - has�o do bazy
     * @return - po��czenie z baz�
     */
    private static Connection connectToDatabase(String adress,
                                                String dataBaseName, String userName, String password)
    {
        String baza = "jdbc:mysql://" + adress + "/" + dataBaseName;
        // objasnienie opisu bazy:
        // jdbc: - mechanizm laczenia z baza (moze byc inny, np. odbc)
        // mysql: - rodzaj bazy
        // adress - adres serwera z baza (moze byc tez w nazwy)
        // dataBaseName - nazwa bazy
        java.sql.Connection connection = null;
        try {
            connection = DriverManager.getConnection(baza, userName, password);
            System.out.println("Polaczono z baza");
        } catch (SQLException e) {
            System.out.println("Nie polaczono z baza!");
            /**
             * utworzenie bazy w przypadku gdy jej nie wykryto
             */
            try
            {
                Connection con=getConnection(adress,3306);
                Statement st = createStatement(con);
                if (executeUpdate(st, "USE "+dataBaseName+";") > -1)
                    System.out.println("Baza wybrana");
                else
                {
                    if (executeUpdate(st, "create Database "+dataBaseName+";") > -1)
                        System.out.println("Baza utworzona");
                    else
                        System.out.println("Baza niewybrana!");
                    connection=connectToDatabase(adress,dataBaseName,userName,password);
                }
            }
            catch (Exception e1)
            {
                connection=connectToDatabase(adress,dataBaseName,userName,password);
            }
        }
        Statement st = createStatement(connection);
        //Sprawdzenie czy tabele istnieja, a jesli nie to utworzenie ich
        try
        {
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "Uzytkownicy", null);
            if (tables.next()) {
                System.out.println("Tabela Uzytkownicy istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Uzytkownicy (login VARCHAR(50) unique NOT NULL, haslo VARCHAR(50) NOT NULL, Imie VARCHAR(50) NOT NULL, Nazwisko VARCHAR(50) NOT NULL, Email VARCHAR(50) unique NOT NULL, typ enum(\"student\",\"prowadzacy\",\"administrator\") NOT NULL, Prowadzone_przedmioty VARCHAR(250), Uczeszczane_przedmioty VARCHAR(250), CzyZatwierdzony tinyint(1) not null);") > -1)
                {
                    System.out.println("Tabela Uzytkownicy utworzona");
                    // utworzenie zatwierdzonego administratora
                    executeUpdate(st, "INSERT INTO uzytkownicy (login, haslo, Imie, Nazwisko, Email, typ ,CzyZatwierdzony ) VALUES ('administrator', 'administrator', 'administrator', 'administrator', 'administrator@admin.pl','administrator', 1); ");
                }
                else
                    System.out.println("Tabela Uzytkownicy nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Przedmioty", null);
            if (tables.next()) {
                System.out.println("Tabela Przedmioty istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Przedmioty (Nazwa VARCHAR(50) unique NOT NULL, Nazwisko_prowadzacego VARCHAR(50) NOT NULL, Preferowany_czas_prowadzacego VARCHAR(50), Godziny_przedmiotu VARCHAR(50), Uczeszczajacy VARCHAR(500) );") > -1)
                    System.out.println("Tabela Przedmioty utworzona");
                else
                    System.out.println("Tabela Przedmioty nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Serwery", null);
            if (tables.next()) {
                System.out.println("Tabela Serwery istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Serwery (IP VARCHAR(50) unique NOT NULL);") > -1)
                    System.out.println("Tabela Serwery utworzona");
                else
                    System.out.println("Tabela Serwery nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Zmiany", null);
            if (tables.next()) {
                System.out.println("Tabela Zmiany istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Zmiany (Tabela VARCHAR(50) NOT NULL, Klucz VARCHAR(50) NOT NULL, KolumnaDoZmiany VARCHAR(50) NOT NULL, NowaWartosc VARCHAR(50) NOT NULL);") > -1)
                    System.out.println("Tabela Zmiany utworzona");
                else
                    System.out.println("Tabela Zmiany nie utworzona!");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * tworzenie obiektu Statement przesy�aj�cego zapytania do bazy connection
     *
     * @param connection
     *            - po��czenie z baz�
     * @return obiekt Statement przesy�aj�cy zapytania do bazy
     */
    private static Statement createStatement(Connection connection) {
        try {
            return connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ;
        return null;
    }

    /**
     * Wykonanie kwerendy i przes�anie wynik�w do obiektu ResultSet
     *
     * @param s
     *            - Statement
     * @param sql
     *            - zapytanie
     * @return wynik
     */
    private static ResultSet executeQuery(Statement s, String sql) {
        try {
            return s.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int executeUpdate(Statement s, String sql) {
        try {
            return s.executeUpdate(sql);
        }
        catch (MySQLIntegrityConstraintViolationException sqle)
        {
            return -5; //5 - dodawanie loginu, lub email który już jest w tabeli
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public void run()
    {
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(csocket.getOutputStream()));
            Menu(in,out);
        }
        catch (IOException ex)
        {
            System.out.println("IOException");
        }
    }

    private void Menu (BufferedReader in, PrintWriter out)
    {
        try
        {
            /**
             * na podstawie pierwszej wyslanej lini serwer decyduje co ma zrobic
             */
            String tekst=in.readLine();
            /**
             * rejestracja
             */
            if(tekst.equals("rejestracja"))
            {
                rejestracja(in,out);
            }
            /**
             * logowanie
             */
            if(tekst.equals("logowanie"))
            {
                logowanie(in,out);
            }
            if(tekst.equals("zmiana_danych"))
            {
                System.out.println("ładowanie danych");
                wyslijDaneDoZmiany(in, out);
            }
            if(tekst.equals("przypomnienie"))
            {
                System.out.println("przypomnienie hasła");
                przypomnienie(in,out);
            }
            if(tekst.equals("zmiana"))
            {
                System.out.println("zmiana danych");
                zmianaDanych(in,out);
            }
            if(tekst.equals("lista_prowadzacych"))
            {
                System.out.println("Wysyłanie listy wykładowców");
                wyslijListeWykladowcow(in,out);
            }
            if(tekst.equals("nowy_przedmiot"))
            {
                System.out.println("Tworzenie nowego przedmiotu");
                nowyPrzedmiot(in,out);
            }
            if(tekst.equals("lista_zajec"))
            {
                System.out.println("Wysyłanie listy zajęć");
                wyslijListeZajec(in,out);
            }
            if(tekst.equals("lista zajec na ktore nie jest zapisany"))
            {
                System.out.println("Wysyłanie listy zajęć");
                wyslijListeZajecNaKtoreNieJestZapisany(in,out);
            }
            if(tekst.equals("lista nieprowadzonych zajec"))
            {
                System.out.println("Wysyłanie listy nieprowadzonych zajęć");
                wyslijListeNieprowadzonychZajec(in,out);
            }
            if(tekst.equals("lista prowadzonych zajec"))
            {
                System.out.println("Wysyłanie listy prowadzonych zajęć");
                wyslijListeProwadzonychZajec(in,out);
            }
            if(tekst.equals("lista niezatwierdzonych"))
            {
                System.out.println("Wysyłanie listy niezatwierdzonych");
                wyslijListeNiezatwierdzonych(in,out);
            }
            if(tekst.equals("zapisz na zajecia"))
            {
                System.out.println("Zapisywanie na zajęcia");
                zapiszNaZajecia(in,out);
            }
            if(tekst.equals("prowadz nowe zajecia"))
            {
                System.out.println("Prowadzenie nowych zajęć");
                prowadzNoweZajecia(in,out);
            }
            if(tekst.equals("zatwierdzanie uzytkownika"))
            {
                System.out.println("Zatwierdzanie użytkownika");
                zatwierdzUzytkownika(in,out);
            }
            if(tekst.equals("zmiana preferowanych godzin"))
            {
                System.out.println("Zmiana preferowanych godzin");
                zmianaPreferowanychGodzin(in,out);
            }
            if(tekst.equals("wylogowanie"))
            {
                System.out.println("wylogowano");
                TypZalogowanego="";
                ObecnieZalogowany="";
                Menu(in,out);
            }
            if(tekst.equals("login szczegoly"))
            {
                System.out.println("Wysyłanie szczegółów konta");
                loginSzczegoly(in,out);
            }
            if(tekst.equals("lista zmian"))
            {
                System.out.println("Wysyłanie listy zmian");
                wyslijListeZmian(in,out);
            }
            if(tekst.equals("zatwierdzanie zmian"))
            {
                System.out.println("Zatwierdzanie zmian");
                zatwierdzZmiane(in,out);
            }
            if(tekst.equals("usuwanie zmian"))
            {
                System.out.println("usuwanie zmian");
                usunZmiane(in,out);
            }
            if(tekst.equals("usuwanie uzytkownika"))
            {
                System.out.println("usuwanie uzytkownika");
                usunUzytkownika(in,out);
            }
            if(tekst.equals("lista uzytkownikow"))
            {
                System.out.println("wysyłanie listy użytkowników");
                wyslijListeUzytkownikow(in,out);
            }
            if(tekst.equals("lista zajec ze szczegolowymi informacjami"))
            {
                System.out.println("wysyłanie szczegółowej listy zajęć");
                wyslijSzczegolowaListeZajec(in,out);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception
    {
        if (ladujSterownik())
            System.out.println(" sterownik OK");
        else
            System.exit(1);
        ServerSocket ssock = new ServerSocket(4255);
        while (true)
        {
            Socket sock = ssock.accept();
            new Thread(new Serwer(sock)).start();
        }
    }

    private void logowanie(BufferedReader in, PrintWriter out)
    {
        String login,hasło;
        try
        {
            login = in.readLine();
            hasło = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE haslo='"+hasło+"' and login='"+login+"';");
            if (wyniklogowania.next())
            {
                boolean zatwierdzony =wyniklogowania.getBoolean("CzyZatwierdzony");
                String typ= wyniklogowania.getString("typ");
                if (zatwierdzony)
                {
                    System.out.println("Udalo sie zalogowac");
                    out.println("poprawne");
                    out.println(typ);
                    TypZalogowanego=typ;
                    ObecnieZalogowany=login;
                }
                else
                {
                    System.out.println("Nie zatwierdzony");
                    out.println("nie zatwierdzony");
                }
            }
            else
            {
                System.out.println("bledne dane");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void rejestracja(BufferedReader in, PrintWriter out)
    {
        String login,hasło,Imie,Nazwisko,Email,typ;
        try
        {
            login = in.readLine();
            hasło = in.readLine();
            Imie = in.readLine();
            Nazwisko = in.readLine();
            Email = in.readLine();
            typ = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            if (executeUpdate(st, "INSERT INTO uzytkownicy (login, haslo, Imie, Nazwisko, Email, typ, CzyZatwierdzony) values ('"+login+"', '"+hasło+"', '"+Imie+"', '"+Nazwisko+"', '"+Email+"', '"+typ+"', 0);") > -1)
            {
                System.out.println("Zarejestrowano uzytkownika");
                out.println("ok");
            }
            else if (executeUpdate(st, "INSERT INTO uzytkownicy (login, haslo, Imie, Nazwisko, Email, typ, CzyZatwierdzony) values ('"+login+"', '"+hasło+"', '"+Imie+"', '"+Nazwisko+"', '"+Email+"', '"+typ+"', 0);") == -5)
            {
                System.out.println("Duplikat loginu lub email");
                out.println("duplikat");
            }
            else
            {
                System.out.println("Nie zarejestrowano uzytkownika!");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

    }

    private void przypomnienie(BufferedReader in, PrintWriter out)
    {
        String email;
        try
        {
            email = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE Email='"+email+"';");
            if (wyniklogowania.next())
            {
                System.out.println("Znaleziono adres email w bazie");
                String login= wyniklogowania.getString("login");
                String haslo= wyniklogowania.getString("haslo");
                try
                {
                    GoogleMail.send(GoogleMail.getGmailService(),email,"","pwj.planlekcji@gmail.com","Pwj - plan lekcji. Przypomnienie hasła","login - "+login+"\nhasło - "+haslo );
                    out.println("poprawne");
                }
                catch (GoogleJsonResponseException e)
                {
                    out.println("zły email");
                }
            }
            else
            {
                System.out.println("bledne dane");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijDaneDoZmiany(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE login='"+ObecnieZalogowany+"';");
            if (wyniklogowania.next())
            {
                String haslo,imie,nazwisko,email;
                haslo= wyniklogowania.getString("haslo");
                imie= wyniklogowania.getString("Imie");
                nazwisko= wyniklogowania.getString("Nazwisko");
                email= wyniklogowania.getString("Email");
                out.println(ObecnieZalogowany);
                out.println(haslo);
                out.println(imie);
                out.println(nazwisko);
                out.println(email);
            }
            else
            {
                System.out.println("Brak zalogowanego uzytkownika w bazie");
                out.println("brak");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void zmianaDanych(BufferedReader in, PrintWriter out)
    {
        try
        {
            String login,hasło,imie,nazwisko,email;
            login=in.readLine();
            hasło=in.readLine();
            imie=in.readLine();
            nazwisko=in.readLine();
            email=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            if (executeUpdate(st, "UPDATE uzytkownicy SET login='"+login+"', haslo='"+hasło+"', Imie='"+imie+"', Nazwisko='"+nazwisko+"', Email='"+email+"' WHERE login='"+ObecnieZalogowany+"';") > -1)
            {
                System.out.println("Zmieniono dane");
                ObecnieZalogowany=login;
                out.println("ok");
            }
            else if (executeUpdate(st, "UPDATE uzytkownicy SET login='"+login+"', haslo='"+hasło+"', Imie='"+imie+"', Nazwisko='"+nazwisko+"', Email='"+email+"' WHERE login='"+ObecnieZalogowany+"';") == -5)
            {
                System.out.println("Duplikat loginu lub email");
                out.println("duplikat");
            }
            else
            {
                System.out.println("Nie zmieniono danych!");
                out.println("bledne");
            }
            out.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Menu(in,out);
    }

    private void wyslijListeWykladowcow(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `uzytkownicy` WHERE typ='prowadzacy';");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
            }
            out.println(size);
            while (wyniklogowania.next())
            {
                String nazwisko;
                nazwisko= wyniklogowania.getString("Nazwisko");
                out.println(nazwisko);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void nowyPrzedmiot(BufferedReader in, PrintWriter out)
    {
        String nazwa,Nazwisko;
        try
        {
            if (!TypZalogowanego.equals("administrator"))
            {
                out.println("brak uprawnien");
            }
            else
            {
                out.println("ok");
                out.flush();
                nazwa = in.readLine();
                Nazwisko = in.readLine();
                Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
                Statement st = createStatement(con);
                if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                    System.out.println("Baza wybrana");
                else
                    System.out.println("Baza niewybrana!");
                if (executeUpdate(st, "Insert into `przedmioty` (Nazwa, Nazwisko_prowadzacego) values ('"+nazwa+"', '"+Nazwisko+"')") > -1)
                {
                    System.out.println("Dodano przedmiot");
                    out.println("ok");
                }
                else if (executeUpdate(st, "Insert into `przedmioty` (Nazwa, Nazwisko_prowadzacego) values ('"+nazwa+"', '"+Nazwisko+"')") == -5)
                {
                    System.out.println("Duplikat nazwy");
                    out.println("duplikat");
                }
                else
                {
                    System.out.println("Nie dodano przedmiotu!");
                    out.println("bledne");
                }
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeZajec(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `przedmioty` WHERE 1;");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String nazwa;
                    nazwa= wyniklogowania.getString("Nazwa");
                    out.println(nazwa);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeZajecNaKtoreNieJestZapisany(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            // przedmioty na które nie jest zapisany
            ResultSet wynik = executeQuery(st, "SELECT * FROM `przedmioty` WHERE Uczeszczajacy NOT LIKE '%"+getNazwiskoZalogowanego()+",%' or Uczeszczajacy is null;");
            int size= 0;
            if (wynik != null)
            {
                wynik.beforeFirst();
                wynik.last();
                size = wynik.getRow();
                wynik.beforeFirst();
                out.println(size);
                while (wynik.next())
                {
                    String nazwa;
                    nazwa= wynik.getString("Nazwa");
                    out.println(nazwa);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeNieprowadzonychZajec(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `przedmioty` where Nazwisko_prowadzacego!='"+getNazwiskoZalogowanego()+"' ;");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String nazwa;
                    nazwa= wyniklogowania.getString("Nazwa");
                    out.println(nazwa);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeProwadzonychZajec(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `przedmioty` where Nazwisko_prowadzacego='"+getNazwiskoZalogowanego()+"' ;");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String nazwa;
                    nazwa= wyniklogowania.getString("Nazwa");
                    out.println(nazwa);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void zapiszNaZajecia(BufferedReader in, PrintWriter out)
    {
        String nazwa,nazwisko;
        try
        {
            nazwisko=getNazwiskoZalogowanego();
            if (!TypZalogowanego.equals("student"))
            {
                out.println("brak uprawnien");
            }
            else
            {
                out.println("ok");
                out.flush();
                nazwa = in.readLine();
                Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
                Statement st = createStatement(con);
                if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                    System.out.println("Baza wybrana");
                else
                    System.out.println("Baza niewybrana!");
                ResultSet wynik = executeQuery(st, "SELECT * FROM `przedmioty` WHERE Nazwa='"+nazwa+"'");
                try
                {
                    if (wynik.next())
                    {
                        String uczeszczajacy= wynik.getString("Uczeszczajacy");
                        if (uczeszczajacy==null)
                        {
                            uczeszczajacy="";
                        }
                        //jest już zapisany na przemiot
                        if (uczeszczajacy.contains(" "+nazwisko+","))
                        {
                            System.out.println("Jest już zapisany na przedmiot!");
                            out.println("duplikat2");
                        }
                        // nie jest zapisany na przedmiot
                        else
                        {
                            try
                            {
                                ResultSet duplikat = executeQuery(st, "SELECT * FROM `zmiany` WHERE Klucz='"+nazwa+"'and Tabela='przedmioty' and KolumnaDoZmiany='Uczeszczajacy' and NowaWartosc='"+nazwisko+"'");
                                // ta zmiana już jest w bazie danych
                                if (duplikat.next())
                                {
                                    System.out.println("Zmiana już jest w bazie danych");
                                    out.println("duplikat");
                                }
                                //dodawane do tabeli zmian w której będzie oczekiwało na zaakceptowanie przez administratora
                                else
                                {
                                    if (executeUpdate(st, "Insert into `zmiany` (Tabela, Klucz, KolumnaDoZmiany, NowaWartosc) values ( 'przedmioty', '"+nazwa+"', 'Uczeszczajacy', '"+nazwisko+"')") > -1)
                                    {
                                        System.out.println("Dodano do kolumny zmian");
                                        out.println("ok");
                                    }
                                    else
                                    {
                                        System.out.println("Nie dodano!");
                                        out.println("bledne");
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Nie znaleziono przedmiotu!");
                        out.println("bledne");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private String getNazwiskoZalogowanego ()
    {
        String nazwisko="";
        Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
        Statement st = createStatement(con);
        if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
            System.out.println("Baza wybrana");
        else
            System.out.println("Baza niewybrana!");
        ResultSet wynik = executeQuery(st, "SELECT * FROM `uzytkownicy` WHERE login='"+ObecnieZalogowany+"'");
        try
        {
            if (wynik.next())
            {
                nazwisko= wynik.getString("Nazwisko");
            }
            else
            {
                System.out.println("Brak zalogowanego uzytkownika w bazie");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return nazwisko;
    }

    private void prowadzNoweZajecia(BufferedReader in, PrintWriter out)
    {
        String nazwa,nazwisko;
        try
        {
            nazwisko=getNazwiskoZalogowanego();
            if (!TypZalogowanego.equals("prowadzacy"))
            {
                out.println("brak uprawnien");
            }
            else
            {
                out.println("ok");
                out.flush();
                nazwa = in.readLine();
                Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
                Statement st = createStatement(con);
                if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                    System.out.println("Baza wybrana");
                else
                    System.out.println("Baza niewybrana!");
                ResultSet wynik = executeQuery(st, "SELECT * FROM `przedmioty` WHERE Nazwa='"+nazwa+"'");
                try
                {
                    if (wynik.next())
                    {
                        String nazwisko_prowadzacego= wynik.getString("Nazwisko_prowadzacego");
                        //jest już zapisany na przemiot
                        if (nazwisko_prowadzacego.equals(nazwisko))
                        {
                            System.out.println("Już prowadzi przedmiot!");
                            out.println("duplikat2");
                        }
                        // nie jest zapisany na przedmiot
                        else
                        {
                            try
                            {
                                ResultSet duplikat = executeQuery(st, "SELECT * FROM `zmiany` WHERE Klucz='"+nazwa+"'and Tabela='przedmioty' and KolumnaDoZmiany='Nazwisko_prowadzacego' and NowaWartosc='"+nazwisko+"'");
                                // ta zmiana już jest w bazie danych
                                if (duplikat.next())
                                {
                                    System.out.println("Zmiana już jest w bazie danych");
                                    out.println("duplikat");
                                }
                                //dodawane do tabeli zmian w której będzie oczekiwało na zaakceptowanie przez administratora
                                else
                                {
                                    if (executeUpdate(st, "Insert into `zmiany` (Tabela, Klucz, KolumnaDoZmiany, NowaWartosc) values ( 'przedmioty', '"+nazwa+"', 'Nazwisko_prowadzacego', '"+nazwisko+"')") > -1)
                                    {
                                        System.out.println("Dodano do kolumny zmian");
                                        out.println("ok");
                                    }
                                    else
                                    {
                                        System.out.println("Nie dodano!");
                                        out.println("bledne");
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Nie znaleziono przedmiotu!");
                        out.println("bledne");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void zmianaPreferowanychGodzin(BufferedReader in, PrintWriter out)
    {
        try
        {
            String nazwa=in.readLine();
            String godziny=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            if (executeUpdate(st, "Insert into `zmiany` (Tabela, Klucz, KolumnaDoZmiany, NowaWartosc) values ( 'przedmioty', '"+nazwa+"', 'Preferowany_czas_prowadzacego', '"+godziny+"')") > -1)
            {
                System.out.println("Dodano do kolumny zmian");
                out.println("ok");
                out.flush();
            }
            else
            {
                System.out.println("Nie zmieniono!");
                out.println("bledne");
                out.flush();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Menu(in,out);
    }

    private void wyslijListeNiezatwierdzonych(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `uzytkownicy` where CzyZatwierdzony="+0);
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String login= wyniklogowania.getString("login");
                    String imie= wyniklogowania.getString("Imie");
                    String nazwisko= wyniklogowania.getString("Nazwisko");
                    String email= wyniklogowania.getString("Email");
                    String typ= wyniklogowania.getString("typ");
                    out.println(login);
                    out.println(imie);
                    out.println(nazwisko);
                    out.println(email);
                    out.println(typ);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void zatwierdzUzytkownika(BufferedReader in, PrintWriter out)
    {
        try
        {
            String login = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            if (executeUpdate(st, "UPDATE `uzytkownicy` SET CzyZatwierdzony=1 where login='"+login+"'") > -1)
            {
                System.out.println("Zatwierdzono użytkownika");
                out.println("ok");
            }
            else
            {
                System.out.println("Nie zatwierdzono użytkownika!");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void loginSzczegoly(BufferedReader in, PrintWriter out)
    {
        try
        {
            String login = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE login='"+login+"';");
            if (wyniklogowania.next())
            {
                out.println("ok");
                String imie= wyniklogowania.getString("Imie");
                String nazwisko= wyniklogowania.getString("Nazwisko");
                String email= wyniklogowania.getString("Email");
                String typ= wyniklogowania.getString("typ");
                out.println(imie);
                out.println(nazwisko);
                out.println(email);
                out.println(typ);
            }
            else
            {
                System.out.println("Brak uzytkownika w bazie");
                out.println("brak");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeZmian(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wynik = executeQuery(st, "SELECT * FROM `zmiany` WHERE 1;");
            int size= 0;
            if (wynik != null)
            {
                wynik.beforeFirst();
                wynik.last();
                size = wynik.getRow();
                wynik.beforeFirst();
                out.println(size);
                while (wynik.next())
                {
                    out.println(wynik.getString("Tabela"));
                    out.println(wynik.getString("Klucz"));
                    out.println(wynik.getString("KolumnaDoZmiany"));
                    out.println(wynik.getString("NowaWartosc"));
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void zatwierdzZmiane(BufferedReader in, PrintWriter out)
    {
        try
        {
            String Tabela, Klucz, Kolumna, Wartosc, NazwaKlucz="";
            Tabela=in.readLine();
            Klucz=in.readLine();
            Kolumna=in.readLine();
            Wartosc=in.readLine();
            String nowaWartosc=Wartosc;
            switch (Tabela)
            {
                case "przedmioty" :
                    NazwaKlucz="Nazwa";
                    break;
                case "uzytkownicy" :
                    NazwaKlucz="login";
                    break;
            }
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");

            // dodawanie użytkowników zmienia, a nie zastępuje dotychczasową wartość w bazie danych
            if (Tabela.equals("przedmioty") && Kolumna.equals("Uczeszczajacy"))
            {
                ResultSet wynik = executeQuery(st, "Select Uczeszczajacy From przedmioty where "+NazwaKlucz+"='"+Klucz+"'");
                wynik.next();
                String lista= wynik.getString("Uczeszczajacy");
                if (lista==null)
                    lista="";
                nowaWartosc=lista+" "+Wartosc+",";
            }
            if (executeUpdate(st, "UPDATE `"+Tabela+"` SET "+Kolumna+"='"+nowaWartosc+"' where "+NazwaKlucz+"='"+Klucz+"'") > -1)
            {
                System.out.println("Wykonano zmiane");
                if (executeUpdate(st, "DELETE FROM `zmiany` WHERE NowaWartosc ='"+Wartosc+"' and Tabela ='"+Tabela+"' and KolumnaDoZmiany ='"+Kolumna+"' and Klucz ='"+Klucz+"' ") > -1)
                {
                    System.out.println("Usunieto zmiane");
                    out.println("ok");
                }
                else
                {
                    System.out.println("Nie usunieto zmiany!");
                    out.println("nie usunieto zmiany");
                }
            }
            else
            {
                System.out.println("Nie wykonano zmiany!");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void usunZmiane(BufferedReader in, PrintWriter out)
    {
        try
        {
            String Tabela, Klucz, Kolumna, Wartosc;
            Tabela=in.readLine();
            Klucz=in.readLine();
            Kolumna=in.readLine();
            Wartosc=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            if (executeUpdate(st, "DELETE FROM `zmiany` WHERE NowaWartosc ='"+Wartosc+"' and Tabela ='"+Tabela+"' and KolumnaDoZmiany ='"+Kolumna+"' and Klucz ='"+Klucz+"' ") > -1)
            {
                System.out.println("Usunieto zmiane");
                out.println("ok");
            }
            else
            {
                System.out.println("Nie usunieto zmiany!");
                out.println("nie usunieto zmiany");
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void usunUzytkownika(BufferedReader in, PrintWriter out)
    {
        try
        {
            String login;
            login=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            if (executeUpdate(st, "DELETE FROM `uzytkownicy` WHERE login ='"+login+"'") > -1)
            {
                System.out.println("Usunieto uzytkownika");
                out.println("ok");
            }
            else
            {
                System.out.println("Nie usunieto uzytkownika!");
                out.println("nie usunieto zmiany");
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijListeUzytkownikow(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `uzytkownicy` where login!='"+ObecnieZalogowany+"'");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String login= wyniklogowania.getString("login");
                    String imie= wyniklogowania.getString("Imie");
                    String nazwisko= wyniklogowania.getString("Nazwisko");
                    String email= wyniklogowania.getString("Email");
                    String typ= wyniklogowania.getString("typ");
                    String czyZatwierdzono=wyniklogowania.getString("CzyZatwierdzony");
                    out.println(login);
                    out.println(imie);
                    out.println(nazwisko);
                    out.println(email);
                    out.println(typ);
                    out.println(czyZatwierdzono);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijSzczegolowaListeZajec(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `przedmioty` WHERE 1;");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
                out.println(size);
                while (wyniklogowania.next())
                {
                    String nazwa= wyniklogowania.getString("Nazwa");
                    String nazwisko= wyniklogowania.getString("Nazwisko_prowadzacego");
                    String godziny= wyniklogowania.getString("Godziny_przedmiotu");
                    int ileStudentow=0;
                    String studentci = wyniklogowania.getString("Uczeszczajacy");
                    // jest tylu uczęszczających na przedmiot ile jest przecinków
                    for(int i = 0; i < studentci.length(); i++) {
                        if(studentci.charAt(i) == ',') ileStudentow++;
                    }
                    out.println(nazwa);
                    out.println(nazwisko);
                    out.println(godziny);
                    out.println(ileStudentow);
                }
            }
            else
            {
                out.println(size);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void ustalGodzinyPrzedmiotu ()
    {
        Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
        Statement st = createStatement(con);
        if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
            System.out.println("Baza wybrana");
        else
            System.out.println("Baza niewybrana!");
        ResultSet wynik = executeQuery(st, "SELECT * FROM `przedmioty`;");
        try
        {
            while (wynik.next())
            {
                String preferowaneGodziny= wynik.getString("Preferowany_czas_prowadzacego");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void wyslijMailZmianaGodzin ()
    {

    }
}
