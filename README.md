# LHA Library for Java


## お知らせ

Nifty.com のサービス終了に伴い、オリジナルのサイトの記事はもうアーカイブ経由でしか参照できません。

- [LHA Library for Java](https://web.archive.org/web/20160915131415/http://homepage1.nifty.com/dangan/Content/Program/Java/jLHA/LhaLibrary.html)  - Internet Archive

こちらはオリジナルのライセンスに従って、その複製を配布するものです。

## 概要

Java 用に書かれた LHA 書庫操作用ライブラリです。
java.util.zip と似たインターフェイスを使用するので、 java.util.zip を扱ったことのある人にとっては簡単に LHA 書庫が操作できます。 

## 動作環境

コンパイルには J2SE SDK 1.2 以降が必要です。
JRE 1.1 以降で実行させる事が出来ます。 ただし、一部で J2SE 1.2 以降で加わった機能を使用しているため、 JRE 1.1 での使用の際にはそれらの機能が利用できません。 そのため JRE 1.1 でも使用できるプログラムを書くためには 若干の注意が必要になります。
HotSpot performance engine もしくは JIT compiler の使用を推奨します。 


## ライセンス

Copyright (C) 2002  Michel Ishizuka  All rights reserved.

```
以下の条件に同意するならばソースとバイナリ形式の再配布と使用を
変更の有無にかかわらず許可する。

１．ソースコードの再配布において著作権表示と この条件のリスト
    および下記の声明文を保持しなくてはならない。

２．バイナリ形式の再配布において著作権表示と この条件のリスト
    および下記の声明文を使用説明書もしくは その他の配布物内に
    含む資料に記述しなければならない。

このソフトウェアは石塚美珠瑠によって無保証で提供され、特定の目
的を達成できるという保証、商品価値が有るという保証にとどまらず、
いかなる明示的および暗示的な保証もしない。
石塚美珠瑠は このソフトウェアの使用による直接的、間接的、偶発
的、特殊な、典型的な、あるいは必然的な損害(使用によるデータの
損失、業務の中断や見込まれていた利益の遺失、代替製品もしくは
サービスの導入費等が考えられるが、決してそれだけに限定されない
損害)に対して、いかなる事態の原因となったとしても、契約上の責
任や無過失責任を含む いかなる責任があろうとも、たとえそれが不
正行為のためであったとしても、またはそのような損害の可能性が報
告されていたとしても一切の責任を負わないものとする。
```

## ダウンロード

https://github.com/IwaoWatanabe/LHA-for-java/archive/refs/heads/main.zip


## 更新履歴

    version 0.06-05 -- 2005年 5月 4日
        [fix] LhaOutputStream で -lhd- のエントリを追加しようとすると、-lh0- にされてしまっていた。
        [fix] LhaProperties で -lhd- のエンコーダとデコーダが設定されていないため、lha.properties が存在しない状態で -lhd- を使用できなかった。 thanx > 吉永さん 

    version 0.06-04 -- 2005年 2月 2日
        [fix] LhaHeader でのファイルサイズの読み込み時に int で読み込んでそのままにしていたため、31ビット値を超えるとファイルサイズが負数になっていたのを修正。 thanx > 金さん 

    version 0.06-03 -- 2004年 6月 28日
        [fix] LhaProperty に記述された lh4～lh7 までの生成式が間違っていたのを修正。 thanx > 山浦さん 

    version 0.06-02 -- 2003年 7月 21日
        [fix] LimitedInputStream の skip でオーバーフローが発生していたのを修正。 thanx > Pierre
        [fix] LhaInputStream の getNextEntry 内で this.limit を渡すべき箇所で の this.in を渡していたのを修正。 thanx > Pierre
        [fix] LhaHeader の exportDirNameExtendHeader 内で System.arraycopy の呼び出しの src と dest の順序を間違えていたのを修正。 thanx > 古賀さん 

    version 0.06-01 -- 2002年 12月 13日
        [fix] 配布ファイルに CompressMethod.java が含まれていなかったのを修正。 thanx > Thomas 

    version 0.06 -- 2002年 12月 11日
        設定ファイルの簡易化
        [fix] PreLh5DecoderFast の ensureBlock() で offLen 部の符号が一つしかない場合 NegativeArraySizeException を投げていた。 thanx > 馬場 さん
        [fix] 配布ファイルに含まれていない ImprovedLzssSearchMethod が ソース内に記述されていたのを削除。 thanx > 佐々木 さん
        [fix] 配布ファイルに LimitedInputStream が含まれていなかったのを修正。 thanx > 勝又 さん 

    version 0.05 -- 2002年 5月 17日
        [fix] jp.gr.java_conf.dangan.util.lha.PreLh5DecoderFast に ミスがあったため、-lh4-, -lh5-, -lh6-, -lh7- で 符号が 1種類しかないため、0ビットで表現する符号の処理の際に ArrayIndexOutOfBoundsException を投げていた。
        [fix] jp.gr.java_conf.dangan.util.lha.PreLh3Decoder に ミスがあったため、-lh3- で 符号が1種類しかないため、 0ビットで表現する符号の処理の際に ArrayIndexOutOfBoundsException を投げていた。 

    version 0.04 -- 2002年 5月 10日
        [add] jp.gr.java_conf.dangan.util.lha.PostLh5EncoderCombo
        [add] jp.gr.java_conf.dangan.util.lha.PreLh5DecoderFast
        [add] jp.gr.java_conf.dangan.io.CachedInputStream
        [add] jp.gr.java_conf.dangan.util.WindowsDate
        [fix] jp/gr/java_conf/dangan/util/lha/resources/lha.properties の 設定が間違っていたため -lh4-, -lh6-, -lh7- での圧縮、解凍時に NoClassDefFoundError を投げていた。
        [fix] jp.gr.java_conf.dangan.util.lha.LzssOutputStream.createSearchReturn に ミスがあったため -lh2-, -lh3-, -lh4-, -lh5-, -lh6-, -lh7- で最長一致を 見つけたとき、一致を無いものとして扱っていた。これによって一部のファイルで 圧縮率が低下していた。
        [fix] jp.gr.java_conf.dangan.util.lha.ChainedHashSearch と jp.gr.java_conf.dangan.util.lha.ChainedHashSearchLimited で 検索可能位置を更新していなかったため圧縮率が低下していた。 

    version 0.03 -- 2002年 4月 15日
        [add] jp.gr.java_conf.dangan.util.lha.LhaRetainedOutputStream
        [fix] jp.gr.java_conf.dangan.util.lha.PreLh3Decoder と jp.gr.java_conf.dangan.util.lha.PostLh3Encoder で -lh3- で 符号が1種類しかないため、0ビットで表現する 符号の読み込み/書き込みの際のビット数を間違えていた。
        [fix] jp.gr.java_conf.dangan.util.lha.DynamicHuffman の addLeaf() の動作が オリジナルのLHAと違っていた。
        [chg] 「public static」な定数を削除しプロパティファイルに よる設定を使用する。(作業途中)
        [chg] jp.gr.java_conf.dangan.io.BitInputStream の protected メソッドを public に変更。
        [chg] jp.gr.java_conf.dangan.io.BitOutputStream の protected メソッドを public に変更。
        [chg] SearchMethod と HashMethod を LzssOutputStream から切り離し、 jp.gr.java_conf.dangan.util.lha.SearchMethod jp.gr.java_conf.dangan.util.lha.ChainedHashSearch jp.gr.java_conf.dangan.util.lha.ChainedHashSearchLimited jp.gr.java_conf.dangan.util.lha.SimpleSearch jp.gr.java_conf.dangan.util.lha.HashMethod jp.gr.java_conf.dangan.util.lha.HashDefault を作成。
        [chg] PreLhaDecoderを削除し、 jp.gr.java_conf.dangan.util.lha.HuffmanDecoder jp.gr.java_conf.dangan.util.lha.StaticHuffmanDecoderTable jp.gr.java_conf.dangan.util.lha.StaticHuffmanDecoderTableAndTree jp.gr.java_conf.dangan.util.lha.StaticHuffmanDecoderTree jp.gr.java_conf.dangan.util.lha.DynamicHuffmanDecoder を作成。 

    version 0.02 -- 2002年 3月 12日
        初版 

----

- 機能追加の要望やバグの報告などありましたら、 cqw10305@nifty.comまでメールしていただけると助かります。

- オリジナルの LHA は吉崎氏によって作成されました。

- 文書内に記述されている社名、製品名については一般に各社の商標または登録商標です。 

----

