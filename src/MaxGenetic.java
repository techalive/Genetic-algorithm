import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
/**
 * 
 * @author Adrian Czarniecki
 *
 */
public class MaxGenetic {

	//funkcja do maksymalizacji
	public static double function(double a, double b, double c, double x) {
		//zeby funkcja na pewno miala jakas wartosc przesuwamy ja sztucznie do gory zeby jej wartosc w 0 byla dodatnia
		return a*x*x + b*x + MaxGenetic.SHIFT; //ta funkcja w 0 ma wartosc 10, ale trzeba ja przy wypisywaniu wyniku przesunac o c-10
		//return a*x*x + b*x + c; to powinno byc gdyby nie bylo przesuwania
	}
	
	//wczytywane z konsoli parametry funkcji kwadratowej
	public static double a = 1.0;
	public static double b = 1.0;
	public static double c = 1.0;
	
	public static int CHROMOSOME_NO = 6; //ilosc osobnikow w kazdej populacji, musi byc parzysta dla krzyzowania parami
	public static double PM = 0.2;		//prawdopodobienstwo mutacji
	public static double PK = 0.8;		//prawdopodobienstwo krzyzowania
	public static int MAX_LOOP = 1000;	//maksymalna ilosc itercji w petli
	public static int MAX_IDLE = 40;	//maksymalna ilosc itercji bez zmiany wartosci najwiekszej, jesli przez tyle iteracji nie zmieni sie wartosc, stopujemy algorytm
	public static int SHIFT = 10;
	
	//zwraca locus w postaci potegi 2 bedacy liczba 1 2 4 8 lub 16 - kazda z tych liczb w zapisie dwojkowym ma tylko jeden bit oznaczajacy wylosowany locus
	public static int getRandomMutationLocus(Random rand) {
		int rnd = rand.nextInt(5);
		int ret = 1;
		for(int i = 0; i < rnd; ++i) ret *= 2;
		return ret;
	}
	
	//zwraca losowy locus do krzyzowania bedacy potega 2 - 2 4 lub 8 lub 16 dzieki odrzuceniu 1 locus jest w srodku 5 bitowego chromosomu, czyli krzyzowanie na pewno zajdzie
	public static int getRandomCrossingLocus(Random rand) {
		int rnd = rand.nextInt(4);
		int ret = 2;
		for(int i = 0; i < rnd; ++i) ret *= 2;		//to sie moze wykonac 0 1 2 lub maksymalnie 3 razy bo rnd jest maksymalnie 3
		return ret;
	}

	/*
	 * W tej metodzie jest caly algorytm genetyczny.
	 */
	public static void findMaximum() {
		
		//potrzebny do losowania generator liczb losowych
		Random random = new Random();
		
		//najpierw tworzymy startowa populacje
		List<Integer> population = new ArrayList<>();
		
		//dodajemy do niej losowe osobniki - liczby losowe z przedzialu [0,31] - takie liczby zapisuja sie dwojkowo na 5 bitach np. 29 = 16 + 8 + 4 + 1= 11101
		for(int i = 0; i < CHROMOSOME_NO; ++i) {
			population.add(random.nextInt(32));
		}
		
		int loopCounter = 0;	//zlicza ilosc wykonanych iteracji petli, bedzie ich nie wiecej niz MAX_LOOP
		int idleCounter = 0;	//zlicza ilosc wykonanych iteracji bez zmiany znalezionej wartosci maksymalnej, przy osiagnieciu MAX_IDLE algorytm stopuje
		double prevMax = Double.NEGATIVE_INFINITY; //znaleziona wartosc maksymalna z poprzedniej iteracji
		double actMax = Double.NEGATIVE_INFINITY;	//wartosc maksymalna z aktualnej iteracji
		
		//LOOP - glowna petla algorytmu - przetwarzanie populacji
		while(true) {	

			List<Double> functionResults = new ArrayList<>(); //lista do przetrzymywania wartosci maksymalizowanej funkcji dla kazdego osobnika z populacji
			double sum = 0.0;	//bedzie zawierac sume wszystkich dodatnich wartosci funkcji dla osobnikow - potrzebna do ustalenia prawdopodobienstw przy losowaniu do nastepnej populacji
			actMax = Double.NEGATIVE_INFINITY;	//ustawiamy actMax na minimalna wartosc, zaraz sie zwiekszy i po przejsciu petli bedzie miec najwieksza wartosci funkcji sposrod wszystkich osobnikow populacji
			for(int i = 0; i < population.size(); ++i) {
				double result = function(a,b,c,population.get(i));	//obliczamy funkcje dla danego osobnika o numerze i
				functionResults.add(result);	//dodajemy wartosc funkcji do listy
				if(result > 0) {	//sumujemy dodatnie wartosci
					sum += result;
				}
				if(result > actMax) {	//obliczamy wartosc najwieksza
					actMax = result;
				}
			}
			
			//aktualizacja idleCountera jesli wartosc maksymalna nie zmienila sie w porownaniu do tej z poprzedniej iteracji
			if(actMax == prevMax) {
				++idleCounter;
			} else {
				idleCounter = 0;
				prevMax = actMax;
			}
			
			//aktualizacja loopCountera
			++loopCounter;
			//WARUNEK STOPU ALGORYTMU
			if(loopCounter >= MaxGenetic.MAX_LOOP || idleCounter >= MaxGenetic.MAX_IDLE) {
				//WYPISANIE ZNALEZIONEJ WARTOSCI NAJWIEKSZEJ
				//maksymalizujemy przesunieta funkcje, wiec tutaj wypisujemy przesunieta wartosc
				System.out.println("MAX: " + (actMax+c-MaxGenetic.SHIFT));
				break;
			}
			
			//System.out.format("%d %d\n", loopCounter, idleCounter);
			for(int i = 0; i < functionResults.size(); ++i) {
				System.out.format("%.2f ", functionResults.get(i) + c - MaxGenetic.SHIFT);
			}
			System.out.println();
			
			//teraz tworzymy liste prawdopodobienst przejscia kazdego osobnika do nastepnej populacji w formie kola ruletki
			//lista ta ma tyle samo elementow ile jest osobnikow w populacji i zawiera rosnace wartosci od 0 do 1 na koncu np. 0.1 0.4 0.7 0.8 0.9 1
			List<Double> probabilities = new ArrayList<>();
			if(sum > 0) {
				double partialSum = 0.0;//skumulowane prawdopodobienstwo dla wszystki osobnikow od 0..i-1
				for(int i = 0; i < functionResults.size(); ++i) {
					//jesli kolejny osobnik o numerze i ma wartosc funkcji dodatnia, to prawdopodobienstwo przejscia go do nastepnej populacji
					//jest rowne wartoscfunkcji/sumacalejpopulacji
					if(functionResults.get(i) > 0) {
						probabilities.add(functionResults.get(i)/sum + partialSum); //tutaj do listy prawdopodobienstw dodajemy to prawdopodobienstwo powiekszone o wartosci dla osobnikow o numerach mniejszych
						partialSum += functionResults.get(i)/sum; //i przygotowujemy sie do nastepnego osobnika
					} else {
						//osobnik z wartoscia ujemna funkcji ma 0 prawdopodobienstwo - w liscie wyglada to np. tak 0.1 0.4 0.4 0.7 0.8 1 - osobnik o numerze 2 (numerowanie od 0) mial ujemna wartosc funkcji
						probabilities.add(partialSum);
					}
				}
			} else {
				//jesli sum <=0 to znaczy ze nie bylo zadnej wartosci dodatniej funkcji dla zadnego osobnika, to nie wyglada dobrze dla maksymalizacji :)
				//w tym przypadku zdecydowalem o rownomiernym rozkladzie prawdopodobienstw dla kazdego osobnika w populacji, np. przy 4 osobnikach kazdy ma prawdopodobienstwo 0.25
				//double partialSum = 0.0;
				for(int i = 0; i < functionResults.size(); ++i) {
					probabilities.add(1.0/functionResults.size()*(i+1));
				}
			}
			
			//majac przygotowana ruletke losujemy nastepna populacje
			List<Integer> nextPopulation = new ArrayList<>();
			//ma ona tyle samo elementow co poprzednia - zawsze CHROMOSOME_NO
			for(int i = 0; i < CHROMOSOME_NO; ++i) {
				double randNo = random.nextDouble();	//losujemy liczbe z przedzialu 0..1
				//i sprawdzamy w ktory przedzial prawdopodobienstwa wpada
				//np. jesli probabilities zawiera liczby 0.1 0.4 0.7 0.8 0.9 1
				for(int j = 0; j < probabilities.size(); ++j) {
					//i wylosujemy 0.75 to ten warunek jest spelniony dla i = 3 (tam gdzie jest 0.8)
					if(probabilities.get(j) >= randNo) {
						//wstawiamy osobnika i do nowej populacji
						Integer newChromosome = population.get(j).intValue(); //tutaj osobnia bedacego liczba w liscie populacja musimy skopiowac, czyli zamienic na int i potem znowu na typ opakowujacy Integer
						nextPopulation.add(newChromosome);
						break;
					}
				}
			}
	
			//POZOSTAJE KROSOWANIE I MUTACJA OSOBNIKOW Z NOWEJ POPULACJI nextPopulation
			
			List<Integer> crossingPopulation = new ArrayList<>();	//bedzie przechowywac osobnikow po krzyzowaniu
			//osobnikow z populacji przechodzimy parami i kazda para moze, ale nie musi ulec krzyzowaniu
			for(int i = 0; i < nextPopulation.size(); i += 2) {
				//a i b to liczy oznaczajace osobnikow do krzyzowania
				int a = nextPopulation.get(i);
				int b = nextPopulation.get(i + 1);
				//tu losujemy liczbe z prawdopodobienstwem PK i jesli zajdzie takie zdarzenie, to krzyzujemy a i b
				if(random.nextDouble() <= PK) {
					int locus = MaxGenetic.getRandomCrossingLocus(random);	//bierzemy losowy locus do krzyzowania bedacy potega 2 : 2,4 lub 8
					//jesli np a = 29 = 11101 a b = 12 = 01100 i locus = 2
					int newa = a - a%locus + b%locus;	//to od a odejmujmey dwa pierwsze od prawej bity czyli 01 i dodajemy dwa pierwsze bity b czyli 00, teraz = 11100
					int newb = b - b%locus + a%locus;	//dla b robimy odwrotnie, czyli po zmianie b = 01101
					//po prostu dwie powyzsze operacje sa zapisem matematycznym operacji bitowych
					//dodajemy nowych osobnikow do populacji po krzyzowaniu
					crossingPopulation.add(newa);
					crossingPopulation.add(newb);
				} else {
					//jesli te dwa osobniki nie zostaly wylosowane do krzyzowania, to ich przy przenoszeniu do nowej populacji nie zmieniamy
					crossingPopulation.add(a);
					crossingPopulation.add(b);
				}
			}
	
			//pozostaje zrobic mutowanie
			List<Integer> mutationPopulation = new ArrayList<>();
			//kazdy osobnik z populacji po krzyzowaniu
			for(int i = 0; i < crossingPopulation.size(); ++i) {
				int a = crossingPopulation.get(i); //pobieramy tego osobnika jako liczbe int
				if(random.nextDouble() <= PM) {
					//i jesli zostal wylosowany do mutowania, losujmy locus bedacy potega 2: 1 2 4 8 16
					int locus = MaxGenetic.getRandomMutationLocus(random);
					//jesli np a=28 i locus = 8 to a%2*8=a%16=12 i poniewaz jest wiecej niz 8 to znaczy ze 8 wystepuje w rozwinieciu 28 na potegi 2 : 28 = 16 + 8 + 4
					if(a % (2*locus) >= locus) {
						//czyli musimy ja usunac z 28, czyli odjac co odpowada zmutowaniu bitu: 28=11100 -> 10100=28-8=10
						mutationPopulation.add(a - locus);
					} else {
						mutationPopulation.add(a + locus);
					}
				} else {
					//podobnie jak przy krzyzowaniu, jesli nie zaszlo zdarzenie o prawdopodobienstwie PM to osobnik nie jest poddawany mutacji
					mutationPopulation.add(a);
				}
			}
			
			//i tak po kolejnych zastepowaniach startowej populacji dochodzimy do ostatniego kroku
			//podmiana starej populacji na nowa
			population = mutationPopulation;
		}
		
	}
	
	public static void main(String args[]) {
		Scanner scr = new Scanner(System.in);
		System.out.println("Podaj wspolczynniki funkcji kwadratowej a,b,c:");
		System.out.print("a = ");
		MaxGenetic.a = scr.nextDouble();
		System.out.print("b = ");
		MaxGenetic.b = scr.nextDouble();
		System.out.print("c = ");
		MaxGenetic.c = scr.nextDouble();
		scr.close();
		
		MaxGenetic.findMaximum();
	}
	
}
