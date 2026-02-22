package com.kindlevibe.reader.reader

import android.content.Context
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

object ReadiumInit {

    lateinit var assetRetriever: AssetRetriever
        private set

    lateinit var publicationOpener: PublicationOpener
        private set

    fun init(context: Context) {
        val httpClient = DefaultHttpClient()

        assetRetriever = AssetRetriever(
            contentResolver = context.contentResolver,
            httpClient = httpClient
        )

        publicationOpener = PublicationOpener(
            publicationParser = DefaultPublicationParser(
                context = context,
                httpClient = httpClient,
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )
    }
}
