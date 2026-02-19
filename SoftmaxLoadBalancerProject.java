import java.util.Arrays;
import java.util.Random;


public class SoftmaxLoadBalancerProject {

    // --- 1. SİMÜLASYON ORTAMI (SERVER) ---
    static class Server {
        int id;
        double meanLatency; // Sunucunun o anki ortalama yanıt süresi (ms)
        Random random = new Random();

        public Server(int id, double initialMeanLatency) {
            this.id = id;
            this.meanLatency = initialMeanLatency;
        }

        // İsteği işle ve yanıt süresi döndür (Gürültülü)
        public double processRequest() {
            // Standart sapması 20ms olan gürültü ekle (Noise)
            double noise = random.nextGaussian() * 20;
            double actualLatency = meanLatency + noise;
            return Math.max(10.0, actualLatency); // Latency negatif olamaz, min 10ms
        }

        // Simülasyon sırasında sunucu performansını değiştir (Non-Stationarity)
        public void setMeanLatency(double newMean) {
            this.meanLatency = newMean;
        }
    }

    // --- 2. SOFTMAX ALGORİTMASI (AGENT) ---
    static class SoftmaxAgent {
        int k; // Sunucu sayısı
        double[] qValues; // Tahmini Ödüller (Rewards)
        double tau; // Sıcaklık (Temperature)
        double alpha; // Öğrenme Oranı (Step-size)
        Random random = new Random();

        public SoftmaxAgent(int k, double tau, double alpha) {
            this.k = k;
            this.tau = tau;
            this.alpha = alpha;
            this.qValues = new double[k];
            // Başlangıç değerleri (Optimistic Initialization)
            // Hepsini başta eşit ve makul bir seviyede başlatıyoruz.
            Arrays.fill(qValues, 10.0);
        }

        public int selectAction() {
            double[] probabilities = new double[k];
            double sumExponentials = 0.0;

            // Numerik stabilite için max Q değerini bul
            double maxQ = Double.NEGATIVE_INFINITY;
            for (double q : qValues) if (q > maxQ) maxQ = q;

            // Boltzmann Dağılımı: P(a) = e^(Q(a)/tau) / sum(...)
            for (int i = 0; i < k; i++) {
                probabilities[i] = Math.exp((qValues[i] - maxQ) / tau);
                sumExponentials += probabilities[i];
            }

            // Olasılığa dayalı seçim (Roulette Wheel)
            double r = random.nextDouble() * sumExponentials;
            double cumulative = 0.0;
            for (int i = 0; i < k; i++) {
                cumulative += probabilities[i];
                if (r <= cumulative) return i;
            }
            return k - 1;
        }

        // Q değerini güncelle: Q_new = Q_old + alpha * (Reward - Q_old)
        public void update(int action, double reward) {
            qValues[action] = qValues[action] + alpha * (reward - qValues[action]);
        }
    }

    // --- 3. ANA SİMÜLASYON (MAIN) ---
    public static void main(String[] args) {
        // Parametreler
        int K = 3; // 3 Sunuculu bir küme
        int TOTAL_REQUESTS = 3000;
        double TAU = 5.0;  // Sıcaklık (Daha yüksek = Daha çok keşif)
        double ALPHA = 0.1; // Sabit adım (Değişen ortamlar için gerekli)

        // Sunucuları başlat
        Server[] servers = new Server[K];
        servers[0] = new Server(0, 50.0);  // Hızlı (50ms)
        servers[1] = new Server(1, 150.0); // Orta (150ms)
        servers[2] = new Server(2, 300.0); // Yavaş (300ms)

        SoftmaxAgent agent = new SoftmaxAgent(K, TAU, ALPHA);

        System.out.println("--- SİMÜLASYON BAŞLIYOR ---");
        System.out.println("Konfigürasyon: Tau=" + TAU + ", Alpha=" + ALPHA);
        System.out.println("Başlangıç Durumu: S0=50ms (En İyi), S1=150ms, S2=300ms");
        System.out.println("-------------------------------------------------------");

        long totalLatency = 0;

        for (int t = 1; t <= TOTAL_REQUESTS; t++) {

            // 1. ADIM: Senaryo Değişimi (Non-Stationary Olayı)
            // 1500. istekte ortam değişiyor!
            if (t == 1500) {
                System.out.println("\n*** DİKKAT: ORTAM DEĞİŞİYOR! ***");
                System.out.println("Sunucu 0 (Eski Hızlı) bozuluyor -> 400ms oluyor.");
                System.out.println("Sunucu 2 (Eski Yavaş) iyileştirildi -> 40ms oluyor.\n");

                servers[0].setMeanLatency(400.0); // Artık çok yavaş
                servers[2].setMeanLatency(40.0);  // Artık en hızlısı bu
            }

            // 2. ADIM: Algoritma Seçimi
            int selectedServerIdx = agent.selectAction();
            Server selectedServer = servers[selectedServerIdx];

            // 3. ADIM: Gerçekleşen Latency ve Ödül
            double latency = selectedServer.processRequest();
            totalLatency += latency;

            // Ödül Fonksiyonu: 1000 / Latency (Latency ne kadar azsa ödül o kadar çok)
            // Örn: 50ms -> 20 puan, 200ms -> 5 puan.
            double reward = 1000.0 / latency;

            // 4. ADIM: Öğrenme (Update)
            agent.update(selectedServerIdx, reward);

            // Loglama (Her 100 istekte bir durum özeti)
            if (t % 300 == 0) {
                printStatus(t, agent.qValues);
            }
        }

        System.out.println("\n--- SONUÇLAR ---");
        System.out.println("Toplam İstek: " + TOTAL_REQUESTS);
        System.out.println("Ortalama Latency: " + (totalLatency / TOTAL_REQUESTS) + " ms");
        System.out.println("Final Q Değerleri (Tahmini Ödüller): " + Arrays.toString(formatArray(agent.qValues)));
        System.out.println("(Not: Q değeri yüksek olan sunucu, algoritmanın 'en hızlı' sandığı sunucudur)");
    }

    // Yardımcı yazdırma fonksiyonu
    private static void printStatus(int step, double[] qValues) {
        System.out.print("Adım " + step + " | Tahmini Ödüller [S0, S1, S2]: ");
        System.out.println(Arrays.toString(formatArray(qValues)));
    }

    private static double[] formatArray(double[] arr) {
        double[] f = new double[arr.length];
        for(int i=0;i<arr.length;i++) f[i] = Math.round(arr[i]*100.0)/100.0;
        return f;
    }
}