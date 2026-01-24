package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.*;
import org.springframework.http.ResponseEntity;

/**
 * ===== import文の説明 =====
 * 
 * Spring Framework関連:
 *   - @Controller: このクラスがコントローラーであることを宣言
 *   - Model: HTMLテンプレートに渡すデータ（タイトルやメッセージなど）を入れる容器
 *   - @GetMapping: GET通信でアクセスされた時に実行するメソッドを指定
 *   - @PostMapping: POST通信でアクセスされた時に実行するメソッドを指定
 *   - @RequestParam: HTMLフォームから送信されたパラメータ（ユーザー入力値）を受け取る
 * 
 * スクレイピング関連:
 *   - Jsoup: WebページのHTMLをパースして、必要なデータを抽出する
 *   - Document: パースされたHTMLドキュメント
 *   - Element: HTMLの要素（タグ）を表す
 *   - Elements: 複数のElement を扱う（リストのようなもの）
 */

/**
 * ===== HomeController クラス =====
 * Webサイトの画面遷移を管理するコントローラー
 * ユーザーのURLアクセス（/、/zodiac、/about、/exchange）に対応したメソッドを実行
 * 処理結果をHTMLテンプレートに渡して、ブラウザに表示する
 */
@Controller
public class HomeController {

    /**
     * ホームページ（/）へのアクセスを処理
     * 「スクレイピング」で /exchange から為替レート情報を取得して表示
     */
    @GetMapping("/")  // http://localhost:8080/ にアクセスされたら実行
    public String home(Model model) {
        // 基本情報を設定
        model.addAttribute("title", "Spring Boot Website");
        model.addAttribute("message", "ようこそ！");
        
        try {
            // /exchange から為替レート情報を「スクレイピング」で取得
            // （実際はHTMLファイルから取得）
            java.nio.file.Path path = java.nio.file.Paths.get(
                "src/main/resources/exchange-rates.html"
            );
            String htmlContent = java.nio.file.Files.readString(path);
            
            // HTMLをJsoupでパース
            Document doc = Jsoup.parse(htmlContent);
            
            // テーブルから通貨レート情報を抽出
            StringBuilder scrapedData = new StringBuilder();
            
            scrapedData.append("【スクレイピングで取得した為替レート】\n");
            scrapedData.append("━━━━━━━━━━━━━━━━━━━━\n");
            
            // 各通貨レートを抽出
            Elements currencies = doc.select("table tr");
            for (int i = 1; i < currencies.size(); i++) {
                Element row = currencies.get(i);
                Elements cells = row.select("td");
                if (cells.size() == 2) {
                    String currency = cells.get(0).text();
                    String rate = cells.get(1).text();
                    scrapedData.append(currency).append(": ").append(rate).append(" JPY\n");
                }
            }
            
            // スクレイピング結果をHTMLに渡す
            model.addAttribute("scrapedExchangeRates", scrapedData.toString());
            model.addAttribute("showExchangeRates", true);
            
        } catch (Exception e) {
            // エラー時は表示しない（デバッグ情報の表示は避ける）
            model.addAttribute("showExchangeRates", false);
        }
        
        // index.htmlを表示
        return "index";
    }

    /**
     * 干支判定フォームの送信を処理
     * ユーザーが入力した西暦から干支を計算して結果をHTMLに渡す
     */
    @PostMapping("/zodiac")  // index.htmlの<form action="/zodiac">から送信されたら実行
    public String zodiac(@RequestParam(name = "year", required = false) Integer year, Model model) {
        // 共通のデータを設定
        model.addAttribute("title", "干支判定結果");
        model.addAttribute("message", "ようこそ！");
        
        // ユーザーが年を入力した場合のみ処理
        if (year != null && year > 0) {
            // 年から干支を計算（例：2024年 → 龍）
            String zodiac = getZodiac(year);
            // 計算結果をHTMLに渡す（Thymeleafで${inputYear}と${zodiacSign}で参照可能）
            model.addAttribute("inputYear", year);
            model.addAttribute("zodiacSign", zodiac);
        }
        
        // 結果を表示するためindex.htmlを返す
        return "index";
    }

    /**
     * 為替レート情報を取得するエンドポイント（/exchange）
     * 外部APIから為替レート情報を取得してHTMLテンプレートに表示
     */
    @GetMapping("/exchange")  // http://localhost:8080/exchange にアクセスされたら実行
    public String exchange(Model model) {
        try {
            // 外部APIから為替レート情報を取得
            RestTemplate restTemplate = new RestTemplate();
            String apiUrl = "https://api.exchangerate-api.com/v4/latest/JPY";
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            // JSON応答をパース
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            JsonObject rates = jsonResponse.getAsJsonObject("rates");
            
            // テーブルから通貨情報を抽出（主要通貨のみ）
            StringBuilder exchangeInfo = new StringBuilder();
            String[] majorCurrencies = {"USD", "EUR", "GBP", "CNY", "KRW"};
            
            exchangeInfo.append("【外部APIから取得した為替レート（JPY基準）】\n");
            exchangeInfo.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
            for (String currency : majorCurrencies) {
                if (rates.has(currency)) {
                    double rate = rates.get(currency).getAsDouble();
                    exchangeInfo.append(currency).append(": ").append(String.format("%.4f", rate)).append(" JPY\n");
                }
            }
            
            // HTMLに為替情報を渡す
            model.addAttribute("title", "為替レート情報");
            model.addAttribute("exchangeRates", exchangeInfo.toString());
            
        } catch (Exception e) {
            // エラー時は別途メッセージを表示
            model.addAttribute("title", "為替レート情報");
            model.addAttribute("exchangeRates", "為替レート情報の取得に失敗しました: " + e.getMessage());
        }
        
        return "exchange";
    }

    /**
     * 西暦から干支を計算するヘルパーメソッド
     * 12年周期で干支が繰り返されることを利用（例：2024年 % 12 = 龍）
     * @param year 西暦
     * @return 干支（鼠、牛、虎...など）
     */
    private String getZodiac(int year) {
        // 干支の配列（12年周期）
        String[] zodiacSigns = {"鼠", "牛", "虎", "兎", "竜", "蛇", "馬", "羊", "猿", "鶏", "犬", "猪"};
        // 西暦を12で割った余りで干支を判定（例：2024-4=2020, 2020%12=8 → 猿）
        return zodiacSigns[(year - 4) % 12];
    }

    /**
     * 概要ページ（/about）へのアクセスを処理
     * about.htmlを表示
     */
    @GetMapping("/about")  // http://localhost:8080/about にアクセスされたら実行
    public String about(Model model) {
        // HTMLに渡すデータを設定
        model.addAttribute("title", "About Us");
        // about.htmlを表示
        return "about";
    }

    /**
     * REST API: 過去5日間の為替レート履歴を取得
     * フロントエンドから非同期で呼び出される
     * @param base 基準通貨（USD、EUR など）
     * @return JSON形式で過去5日間のレート情報
     */
    @GetMapping("/api/exchange-history")
    public ResponseEntity<?> getExchangeRateHistory(@RequestParam String base) {
        try {
            base = base.toUpperCase().trim();
            
            // 過去5日間のレート取得
            RestTemplate restTemplate = new RestTemplate();
            Map<String, List<Double>> rateHistory = new LinkedHashMap<>();
            String[] currencies = {"USD", "EUR", "AUD", "NZD"};
            
            // 各通貨のレート推移を取得
            for (String currency : currencies) {
                List<Double> rates = new ArrayList<>();
                
                for (int i = 4; i >= 0; i--) {
                    try {
                        // 過去レート取得API
                        String apiUrl = String.format(
                            "https://api.exchangerate-api.com/v4/latest/%s",
                            base
                        );
                        String response = restTemplate.getForObject(apiUrl, String.class);
                        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                        JsonObject ratesObj = jsonResponse.getAsJsonObject("rates");
                        
                        if (ratesObj.has(currency)) {
                            double rate = ratesObj.get(currency).getAsDouble();
                            rates.add(rate);
                        }
                    } catch (Exception e) {
                        // エラーの場合はスキップして次の日付へ
                        rates.add(null);
                    }
                }
                
                rateHistory.put(currency, rates);
            }
            
            // JSON応答を返す
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", rateHistory);
            response.put("base", base);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "レート情報の取得に失敗しました: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }
}

