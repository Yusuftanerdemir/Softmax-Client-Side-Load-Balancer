# ⚖️ Softmax Client-Side Load Balancer

Selamlar! 👋 Bu repoda, dağıtık sistemlerde (distributed systems) sıkça karşılaşılan "zamanla performansı değişen (non-stationary) ve gürültülü (noisy)" sunucu ortamları için geliştirdiğim istemci taraflı bir yük dengeleyici (load balancer) bulunuyor.

Klasik Round-Robin veya Random algoritmalarının yetersiz kaldığı, sunucuların aniden yavaşlayıp hızlanabildiği senaryolarda sistemin nasıl otonom bir şekilde adapte olduğunu simüle ediyoruz.

## 🎯 Neden Softmax? (Round-Robin Neden Yetmiyor?)

Normalde yük dengeleyiciler istekleri sırayla (Round-Robin) dağıtır. Ama ya sunuculardan biri aniden arızalanır veya çok yavaşlarsa? Round-Robin kör bir şekilde o yavaşlayan sunucuya istek atmaya devam eder ve toplam bekleme süresini (latency) mahveder.

Bu projede **Softmax Action Selection** algoritmasını kullanarak, sistemin geçmiş performans verilerine (gecikme sürelerine) bakarak matematiksel bir olasılık dağılımı oluşturmasını sağladım. Algoritma:
- En hızlı sunucuyu keşfeder ve trafiğin çoğunu oraya yönlendirir (**Exploitation**).
- Geri kalan ihtimallerle diğer sunucuları yoklayarak durumlarının düzelip düzelmediğini keşfeder (**Exploration**).

## ⚙️ Teknik Detaylar ve Çözülen Problemler

Projeyi geliştirirken sadece teorik formülü koda dökmedim, aynı zamanda gerçek dünya problemlerine çözümler ürettim:

* **Nümerik Stabilite (Numerical Stability):** Softmax formülündeki üs alma (e^x) işlemi, bilgisayar sistemlerinde büyük sayılarda belleği taşırıp (Overflow) algoritmayı çökertebilir. Bunu engellemek için kod içerisinde **Softmax Trick** uyguladım (işlem öncesi tüm değerlerden maksimum Q değerini çıkarma mantığı).
* **Sabit Adım (Constant Alpha):** Ortam aniden değiştiğinde (eski hızlı sunucu bozulduğunda), algoritmanın geçmişi unutup yeni duruma adapte olması için klasik aritmetik ortalama yerine üstel hareketli ortalama sağlayan sabit bir öğrenme oranı (alpha) kullandım.
* **Ödül Tasarımı (Reward Shaping):** Gecikmeyi (latency) doğrudan kullanmak yerine, `1000 / latency` formülüyle gecikmeyi bir "ödüle" (reward) dönüştürdüm.

## 🚀 Nasıl Çalıştırılır?

Proje dış bir kütüphane (dependency) gerektirmez. Saf Java ile, tek bir dosya halinde çalışacak şekilde kurgulanmıştır.

1. Repoyu klonlayın.
2. Terminal veya komut satırında dosyanın bulunduğu dizine gidin.
3. Derleyin ve çalıştırın:
   ```bash
   javac SoftmaxLoadBalancerProject.java
   java SoftmaxLoadBalancerProject
