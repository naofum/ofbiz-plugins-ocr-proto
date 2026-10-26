# ocr プラグイン詳細仕様

## 1. スコープ

本仕様書は ocr プラグインの Step 0 を対象とする。  
Step 0 で提供する機能は以下の通り。

- Tesseract を使った「画像/PDF → テキスト」抽出
- OpenAI 互換 API を使った「画像/PDF → 構造化データ」抽出
- `DataResource` / `Content` 経由のファイル入力
- `ByteBuffer` 直接入力サービス
- 最小限の WebUI（アップロード・エンジン選択・結果表示）
- 単体テスト（モック）

ウィジェット提供、非同期実行、API キー暗号化は Step 0 では対象外。

## 2. 用語定義

| 用語 | 説明 |
|---|---|
| OCR Request | OCR 実行を表すレコード。`OcrRequest` エンティティ。 |
| OCR Result | OCR 実行結果。`OcrRequestResult` エンティティ。 |
| OCR Engine | 実際に認識処理を行うエンジン。Tesseract または OpenAI 互換 API。 |
| Mapping Definition | AI モデルに対するプロンプトと期待する出力項目の定義。`OcrMappingDefinition` エンティティ。 |
| Engine Config | エンジン接続設定。`OcrEngineConfig` エンティティ。 |

## 3. アーキテクチャ

```
┌─────────────────────────────────────────────┐
│              呼び出し元                      │
│  (他プラグイン / WebUI / Groovy サービス)    │
└──────────────┬──────────────────────────────┘
               │ サービス呼び出し
               ▼
┌─────────────────────────────────────────────┐
│          OcrServices (Service facade)       │
│  ocrImageToText / ocrImageToStructuredData  │
│  ocrBytesToText / ocrBytesToStructuredData  │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│           OcrEngineFactory                  │
│      engineType → OcrEngine 解決            │
└───────┬───────────────────────┬─────────────┘
        ▼                       ▼
┌───────────────┐      ┌──────────────────┐
│ TesseractOcr  │      │ OpenAiOcrEngine  │
│   Engine      │      │                  │
└───────────────┘      └──────────────────┘
```

## 4. エンティティ

### 4.1 OcrRequest

OCR 実行要求を表す。

| フィールド | 型 | 説明 |
|---|---|---|
| `ocrRequestId` | ID | 主キー。自動採番。 |
| `contentId` | String | 入力ファイルの Content ID。`ByteBuffer` 入力の場合は null。 |
| `engineTypeId` | String | `TESSERACT` / `OPENAI` / `BOTH`。	|
| `mappingDefinitionId` | String | 構造化データ抽出時のマッピング定義。テキスト抽出時は null。 |
| `inlinePrompt` | String | インラインプロンプト。mappingDefinitionId 指定時は無視。 |
| `language` | String | Tesseract 用言語コード（例: `jpn` `eng`）。 |
| `statusId` | String | `OCRREQ_PENDING` / `OCRREQ_SUCCESS` / `OCRREQ_ERROR`。 |
| `persistResult` | String (`Y`/`N`) | 結果を永続化するかどうか。 |
| `createdByUserLoginId` | String | 実行ユーザー。 |
| `createdTxStamp` | Timestamp | 作成日時。 |

### 4.2 OcrRequestResult

OCR 実行結果を表す。

| フィールド | 型 | 説明 |
|---|---|---|
| `ocrRequestResultId` | ID | 主キー。 |
| `ocrRequestId` | String | 親の OCR Request。 |
| `rawText` | Long Text | 生のテキスト結果。 |
| `structuredResult` | Long Text | 構造化データの JSON 表現。 |
| `confidence` | Numeric | 信頼度（Tesseract の場合）。 |
| `errorMessage` | String | エラー発生時のメッセージ。 |
| `createdTxStamp` | Timestamp | 作成日時。 |

### 4.3 OcrEngineConfig

エンジン設定を表す。

| フィールド | 型 | 説明 |
|---|---|---|
| `ocrEngineConfigId` | ID | 主キー。 |
| `engineTypeId` | String | `TESSERACT` / `OPENAI`。 |
| `configName` | String | 設定名。 |
| `baseUrl` | String | OpenAI 互換 API のベース URL。 |
| `apiKey` | String | API キー（平文）。 |
| `modelName` | String | モデル名（例: `gpt-4o`）。 |
| `tessDataPath` | String | Tesseract の言語データパス。 |
| `language` | String | 既定の言語コード。 |

### 4.4 OcrMappingDefinition

構造化データ抽出用のマッピング定義。

| フィールド | 型 | 説明 |
|---|---|---|
| `ocrMappingDefinitionId` | ID | 主キー。 |
| `definitionName` | String | 定義名。 |
| `prompt` | Long Text | AI モデルへのプロンプト。 |
| `outputFields` | Long Text | 期待する出力項目の JSON（例: `[{"name":"amount","type":"BigDecimal"}]`）。 |
| `description` | String | 説明。 |

### 4.5 フィールド型の実装上の注意

- 主キーは OFBiz の `id` 型（PostgreSQL では `character varying(20)`）を使用する。20 文字を超える ID は `id-long` 等を検討する。
- 人が読む名称フィールド（`configName`, `definitionName`）は `long-varchar` 型を使用し、20 文字以上の名前も格納できるようにする。
- 長いテキスト（`rawText`, `structuredResult`, `prompt`, `outputFields`）は `long-varchar` または `very-long` 型を使用する。
- デモデータの ID は `id` 型の長さ制限に収まるよう命名する（例: `OCR_CFG_TESS_DEMO`）。

## 5. サービス

### 5.1 ocrImageToText

`DataResource` / `Content` 経由で画像/PDF を受け取り、テキストを返す。

| 入力 | 型 | 必須 | 説明 |
|---|---|---|---|
| `contentId` | String | ○ | Content ID。 |
| `engineTypeId` | String | ○ | `TESSERACT` / `OPENAI` / `BOTH`。 |
| `language` | String | - | Tesseract 用言語。 |
| `ocrEngineConfigId` | String | - | 設定 ID。未指定時はデフォルト設定。 |
| `persistResult` | String | - | `Y` の場合結果を永続化。既定 `N`。 |

| 出力 | 型 | 説明 |
|---|---|---|
| `ocrRequestId` | String | 作成された OCR Request ID。 |
| `rawText` | String | 抽出されたテキスト。 |
| `errorMessage` | String | エラー時のメッセージ。 |

### 5.2 ocrImageToStructuredData

`DataResource` / `Content` 経由で画像/PDF を受け取り、構造化データを返す。

| 入力 | 型 | 必須 | 説明 |
|---|---|---|---|
| `contentId` | String | ○ | Content ID。 |
| `engineTypeId` | String | ○ | `OPENAI`（`TESSERACT` は構造化不可）。 |
| `mappingDefinitionId` | String | △ | マッピング定義 ID。 |
| `inlinePrompt` | String | △ | インラインプロンプト。`mappingDefinitionId` 未指定時は必須。 |
| `outputFields` | List | - | インラインプロンプト使用時の出力項目定義。 |
| `ocrEngineConfigId` | String | - | 設定 ID。 |
| `persistResult` | String | - | `Y` の場合結果を永続化。既定 `N`。 |

| 出力 | 型 | 説明 |
|---|---|---|
| `ocrRequestId` | String | 作成された OCR Request ID。 |
| `rawText` | String | AI からの生の応答テキスト。 |
| `structuredResult` | Map | パース済みの構造化データ。 |
| `errorMessage` | String | エラー時のメッセージ。 |

### 5.3 ocrBytesToText / ocrBytesToStructuredData

`ByteBuffer` でファイルデータを直接受け取る版。  
入力に `byteBuffer` と `fileName` を持ち、それ以外は `ocrImageToText` / `ocrImageToStructuredData` と同じ。

## 6. OCR エンジン実装

### 6.1 共通インターフェース

```java
public interface OcrEngine {
    OcrResult recognizeText(OcrInput input) throws OcrException;
    OcrResult recognizeStructured(OcrInput input, OcrMappingDefinition mapping) throws OcrException;
}
```

### 6.2 TesseractOcrEngine

- Tess4J 5.20.0 を使用。
- PDF の場合は PDFBox で画像化してから認識。
- `tessdata` パスを `OcrEngineConfig.tessDataPath` または `ocr.properties` の `tesseract.dataPath` から取得。
- 言語コードを `language` パラメータまたは設定から取得。

### 6.3 OpenAiOcrEngine

- Apache HttpClient 4.5.14 を使用。
- Jackson 2.21.3 を使用して JSON シリアライズ/デシリアライズ。
- OpenAI 互換チャット完了 API を呼び出す。
- 画像は base64 エンコード（データURI形式）してリクエストに含める。
- PDF は PDFBox で画像化し、ページごとに画像を送信するか、テキスト化してプロンプトに含める。
- レスポンスから `choices[0].message.content` を取得し、JSON としてパースする。
- パース結果は `Map<String, Object>` に変換して返す。

## 7. ファイル処理フロー

1. サービスが入力を受け取る。
2. `Content` 入力の場合は `ContentWorker` で `byte[]` を取得。`ByteBuffer` 入力の場合はそのまま使用。
3. MIME タイプまたは拡張子で画像/PDF を判定。
4. PDF の場合は `PdfToImageConverter` で画像リストに変換。
5. 選択された `OcrEngine` に入力を渡す。
6. 結果を `OcrResult` として受け取る。
7. `persistResult=Y` の場合、`OcrRequestResult` を作成。
8. サービス出力として返す。

## 8. UI

### 8.1 WebUI 画面

| 画面 | 説明 |
|---|---|
| `FindOcrRequests` | OCR 実行履歴一覧。 |
| `EditOcrRequest` | OCR 実行詳細。結果を表示。 |
| `UploadOcrFile` | ファイルアップロードとエンジン選択、実行。 |

### 8.2 アップロードフロー

1. ユーザーがファイルをアップロード。
2. OFBiz の `DataResource` / `Content` を作成。
3. 選択されたサービス（`ocrImageToText` または `ocrImageToStructuredData`）を実行。
4. 結果画面に遷移。

## 9. セキュリティ

- Permission:
  - `OCR_ADMIN`: 設定・定義の管理。
  - `OCR_CREATE`: OCR 実行。
  - `OCR_VIEW`: 結果閲覧。
- Security Group:
  - `OCR_USER`: `OCR_CREATE`, `OCR_VIEW`
  - `OCR_ADMINISTRATOR`: `OCR_ADMIN`, `OCR_CREATE`, `OCR_VIEW`

## 10. 設定

`config/ocr.properties`:

```properties
# デフォルトの Tesseract 言語データパス
ocr.tesseract.dataPath=

# デフォルトの Tesseract 言語
ocr.tesseract.language=eng

# デフォルトの OpenAI 互換 API ベース URL
ocr.openai.baseUrl=https://api.openai.com/v1

# デフォルトモデル
ocr.openai.model=gpt-4o

# タイムアウト（ミリ秒）
ocr.openai.timeoutMillis=30000
```

## 11. テスト方針

- OFBiz の `ofbiz --test` ランナーはメインクラスパスからテストを読み込むため、テストクラスは `src/main/java/org/apache/ofbiz/ocr/test/` に配置する。
- `OFBizTestCase` を継承し、JUnit 3 形式（コンストラクタ `TestName(String)`、`test*` メソッド）で記述する。
- `OcrEngine` のモック実装を作成。
- `OcrServices` の単体テストを作成。
- PDF → 画像変換のテストは、テスト用 PDF リソースを使用。
- OpenAI API 呼び出しは `HttpClient` をモック化。
- Tesseract のテストは、Tesseract がインストールされている環境でのみ実行する統合テストとして分離。

## 12. 今後の拡張（Step 1 以降）

- ウィジェット提供（`OcrScreens.xml` / `OcrForms.xml` を再利用可能に）
- API キーの暗号化保存
- 非同期 OCR 実行（`JobScheduler`）
- マルチページ PDF の一括処理最適化
- 新しい OCR エンジンの追加（Google Cloud Vision 等）
