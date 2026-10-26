# AGENTS.md

## Project Context

- Apache OFBiz Web アプリケーション向け OCR 汎用基盤プラグイン。
- 開発対象は `plugins/ocr/` にある。
- 要件定義は `REQUIREMENTS.md`、詳細仕様は `SPEC.md` を正とする。
- Windows 環境では `gradlew.bat` を使用する。
- このプラグイン自体はビジネスロジックを持たず、他のアプリケーションやプラグインから呼び出される OCR サービスを提供する。

## Layer Rules

- エンジン実装は `org.apache.ofbiz.ocr.common` 配下の `OcrEngine` インターフェースに従う。
- 新しい OCR エンジンを追加する場合は `OcrEngine` を実装し、`OcrEngineFactory` に登録する。
- サービス層は `OcrServices` を中心とし、エンジン specifics はエンジン実装に閉じる。
- ファイル入出力は `OcrInputHelper` 等の共通ヘルパーで統一し、サービスコードに直接 Content/ByteBuffer の変換ロジックを書かない。

## Data Rules

- `data/*TypeData.xml` は seed、`data/*DemoData.xml` は demo、`testdef/data/*` はテスト専用。用途を混在させない。
- API キーは Step 0 では `OcrEngineConfig` に平文で保存する。暗号化は Step 1 以降で対応。
- ステータス demo データは業務スナップショットとして扱い、本番の状態遷移制約を緩和しない。

## Validation

- XML 変更時は整形式を確認する。
- `ofbiz --test` ランナーはメインクラスパスからテストを読み込むため、テストクラスは `src/main/java/org/apache/ofbiz/ocr/test/` に配置し、`OFBizTestCase` を継承する。
- Java/Groovy 変更時は以下を実行する。

  ```powershell
  .\gradlew.bat compileJava compileGroovy
  .\gradlew.bat "ofbiz --test component=ocr"
  ```

- テスト前に通常の OFBiz が停止していることを確認する。テスト用 OFBiz はコマンドが起動・停止する。
- テスト結果は `runtime/logs/test-results/ocrtests.xml` で `errors="0"` と `failures="0"` を確認する。
- 新しい seed ファイルを追加・登録した場合は、テスト前に以下を実行する。

  ```powershell
  .\gradlew.bat "ofbiz --load-data readers=seed --load-data component=ocr"
  ```

- demo データを変更した場合は、必要に応じて以下を実行して実ロードを確認する。

  ```powershell
  .\gradlew.bat "ofbiz --load-data readers=demo --load-data component=ocr"
  ```

## Change Boundaries

- OCR 機能は原則として `plugins/ocr/` に実装する。
- core 変更が必要な場合は、理由、影響範囲、代替案、検証結果を `SPEC.md` に記録する。

## Repository and Database Safety

- この作業ツリーでは Git メタデータが利用できない場合がある。`git diff` を前提にせず、対象ファイル、実ロード、コンパイル、テスト結果で検証する。
- 旧テスト DB テーブルや反復開発で残った列は、明示的な依頼なしに削除しない。
- 既知の旧テーブル・列に関する警告は、現行 entity、データロード、ocr テストへの影響を確認してから扱う。
