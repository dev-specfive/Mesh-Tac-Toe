package com.spark.app_main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.spark.app.ui.ScreenFragment
import com.spark.app_main.databinding.FragmentShopBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ShopFragment : ScreenFragment("Shop") {
    private lateinit var binding: FragmentShopBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.webview.loadShopContent("https://specfive.com/collections/all")

    }

    private fun WebView.loadShopContent(url: String) {
        webViewClient = WebViewClient()
        val webSettings = settings
        webSettings.javaScriptEnabled = true
        loadUrl(url)


    }

}