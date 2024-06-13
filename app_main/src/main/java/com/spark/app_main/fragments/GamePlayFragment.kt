package com.spark.app_main.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.spark.app.DataPacket
import com.spark.app.MessageStatus
import com.spark.app.NodeInfo
import com.spark.app.R
import com.spark.app.database.entity.Packet
import com.spark.app.model.UIViewModel
import com.spark.app.service.InviteState
import com.spark.app.ui.ScreenFragment
import com.spark.app.util.GridSpacingItemDecoration
import com.spark.app_main.adapters.GamePlayAdapter
import com.spark.app_main.adapters.TickTacToeEnum
import com.spark.app_main.adapters.TickTackToeOptionState
import com.spark.app_main.data.prefs.PreferencesHelperImpl
import com.spark.app_main.databinding.FragmentNewTikTacToeGameBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Arrays
import javax.inject.Inject

@AndroidEntryPoint
class GamePlayFragment : ScreenFragment("GamePlayFragment") {
    private lateinit var _binding: FragmentNewTikTacToeGameBinding

    @Inject
    lateinit var pref: PreferencesHelperImpl

    private val gamePlayAdapter = GamePlayAdapter { index, state ->
        playIfOppenentMadeMove(index)
    }

    companion object {

        // Player representation
        // 0 - X
        // 1 - O
        var activePlayer = 0
        var gameActive = true
        var lastMessageFromLocal: Boolean = false
        var lastReceivedMSG: String? = null // To prevent receiving message again

        // State meanings:
        //    0 - X
        //    1 - O
        //    2 - Null
        // put all win positions in a 2D array
        var winPositions = arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6)
        )
    }


    var gameState = intArrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2)
    var counter = 0

    private val model: UIViewModel by activityViewModels()
    private var contactKey = ""
    private lateinit var ownPlayerName: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewTikTacToeGameBinding.inflate(inflater, container, false)
        return _binding.root
    }

    private val matrixRowCol = 3

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionbarTitle()

        // Clear the list if user enter into the game
        GlobalScope.launch {
            pref.saveList(emptyList())
        }
        val spacing = 40
        val includeEdge = true
        val borderColor = Color.parseColor("#DBDBDB")
        val customGridDecoration = GridSpacingItemDecoration(
            matrixRowCol,
            spacing,
            includeEdge,
            borderColor = borderColor
        )
        // Lines Preview checking on app
//        GlobalScope.launch {
//            for (i in 0..7) {
//                withContext(Dispatchers.Main) {
//                    drawHorizontalWinState(i)
//                }
//                delay(3000)
//            }
//        }


        customGridDecoration.setWinPositions(winPositions)
        _binding.recyclerview.apply {
            addItemDecoration(
                customGridDecoration
            )
            layoutManager = GridLayoutManager(requireContext(), matrixRowCol)
            adapter = gamePlayAdapter
            val list = List(matrixRowCol * matrixRowCol) { TickTackToeOptionState() }
            gamePlayAdapter.submitList(list)
        }

        observers()
        clickListeners()
        gameReset("")
    }

    private fun setUpActionbarTitle() {
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(true)
        updateToolbarNavigationIconColor()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Back"
    }

    private fun updateToolbarNavigationIconColor() {
        val activity = requireActivity() as AppCompatActivity
        val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
        toolbar.navigationIcon =
            context?.let { ContextCompat.getDrawable(it, R.drawable.arrow_back) }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Hide toolbar title when fragment is destroyed
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun onResume() {
        super.onResume()
        getInvitedChannelName()
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            LocalBroadcastManager.getInstance(it)
                .registerReceiver(
                    leftGameReceiver,
                    IntentFilter(InviteState.LEFT_GAME.title)
                )
        }
    }

    fun clickListeners() {
        _binding.playAgain.setOnClickListener {

            val turnSwitch = if (gameInviteAcceptMsg.equals("X", true)) "O-X" else "X-O"
            val str = "playAgain-$turnSwitch"
            gameInviteAcceptMsg = str.split("-")[1]
            model.sendMessage(str, contactKey)

            Handler(Looper.getMainLooper()).postDelayed({
                startGame(msgFrom, str.split("-"))
            }, 5000)
        }

    }


    fun observers() {
        model.nodeDB.nodeDBbyNum.asLiveData().observe(viewLifecycleOwner) {
            val nodes = it.values.toTypedArray() as Array<NodeInfo>
            if (nodes.size > 1)
                nodes.sortWith(compareByDescending { it.lastHeard }, 1)
            _binding.playerName.text = nodes.firstOrNull()?.user?.longName ?: ""

        }

        model.messages.observe(viewLifecycleOwner) {
            messageReceived(it)
        }
    }

    private fun getInvitedChannelName() {
        context?.let { ctx ->
            val channel = model.channelRequestsAccepted.value?.entries?.firstOrNull { it.value }
            channel?.key?.let {
                contactKey = channel.key.toString()
                model.setContactKey(contactKey)
            }

        }
    }

    private fun drawHorizontalWinState(linePosition: Int) {
        val horizontalLineView = _binding.horizontalLineView
        horizontalLineView.setPosition(linePosition)
        horizontalLineView.visibility = if (linePosition != -1) View.VISIBLE else View.GONE
    }

    private fun setPlayerName(inviteMsg: List<String>, from: String?) {
        inviteMsg[1].apply {
            if (::ownPlayerName.isInitialized.not())
                ownPlayerName = if (from == DataPacket.ID_LOCAL) {
                    this
                } else {
                    if (equals(UIViewModel.Player.X.name, true)) UIViewModel.Player.O.name
                    else UIViewModel.Player.X.name
                }
            val greenOrBlue = if (ownPlayerName.equals("x", true)) "blue" else "green"
            val playerText = "You are team $greenOrBlue"

            val spannableText = SpannableStringBuilder(playerText)

            // Define the start and end index of the color
            val startIndex = playerText.indexOf(greenOrBlue)
            val endIndex = startIndex + greenOrBlue.length

            // Set the color spans
            context?.let {
                val color = if (greenOrBlue == "blue") ContextCompat.getColor(
                    it,
                    R.color.blue_tile_color
                ) else ContextCompat.getColor(it, R.color.green_tile_color)
                val colorSpan = ForegroundColorSpan(color)
                spannableText.setSpan(
                    colorSpan,
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                _binding.playerTurn.text = spannableText
                _binding.playerTurn.visibility = View.VISIBLE
            }

        }
        _binding.tvResult.text = "${inviteMsg[2]}'s Turn - Tap to play".switchTextBlueGreen()
        setActivePlayer(inviteMsg[2])
    }

    private fun playIfOppenentMadeMove(index: Int) {
        synchronized(this) {
            if (!lastMessageFromLocal) {
                lastMessageFromLocal = true
                playerTap(index)
            } else
                Toast.makeText(context, "Please wait for the opponent's move", Toast.LENGTH_SHORT)
                    .show()
        }
    }

    // this function will be called every time a
    // players tap in an empty box of the grid
    fun playerTap(index: Int) {
        getInvitedChannelName()
        if (contactKey.isNotBlank()) {

            // game reset function will be called
            // if someone wins or the boxes are full
            if (!gameActive) {
                gameReset("X's Turn - Tap to play".switchTextBlueGreen())
                //Reset the counter
            }

            // if the tapped image is empty
            if (gameState[index] === 2) {
                // increase the counter
                // after every tap
                counter++

                // check if its the last box
                if (counter == 9) {
                    // reset the game
                    gameActive = false
                }

                // mark this position
                gameState[index] = activePlayer


                // change the active player

                // from 0 to 1 or 1 to 0
                if (activePlayer == 0) {
                    // set the image of x
                    gamePlayAdapter.setMove(index, TickTacToeEnum.Blue)
                    activePlayer = 1
                    val tempMsg = "X:${index}"
                    sendRadioMessage(tempMsg)
                    // change the status
                    _binding.tvResult.text = "O's Turn - Tap to play".switchTextBlueGreen()
                } else {
                    // set the image of o
                    gamePlayAdapter.setMove(index, TickTacToeEnum.Green)
                    activePlayer = 0
                    val tempMsg = "O:${index}"
                    sendRadioMessage(tempMsg)
                    // change the status
                    _binding.tvResult.text = "X's Turn - Tap to play".switchTextBlueGreen()
                }
            } else
                lastMessageFromLocal = false
            var flag = 0
            // Check if any player has won if counter is > 4 as min 5 taps are
            // required to declare a winner
            if (counter > 4) {
                for (winPosition in winPositions) {
                    if (gameState[winPosition[0]] === gameState[winPosition[1]] &&
                        gameState[winPosition[1]] === gameState[winPosition[2]] &&
                        gameState[winPosition[0]] !== 2
                    ) {
                        flag = 1

                        // Somebody has won! - Find out who!
                        var winnerStr: String

                        // game reset function be called
                        gameActive = false
                        winnerStr = if (gameState[winPosition[0]] === 0) {
                            "X has won".switchTextBlueGreen()

                        } else {
                            "O has won".switchTextBlueGreen()
                        }
                        val winStateIndex = winPositions.indexOf(winPosition)
                        drawHorizontalWinState(winStateIndex)

                        // Update the status bar for winner announcement
                        _binding.tvResult.text = winnerStr
                        _binding.playAgain.visibility = View.VISIBLE
//                        gameReset(winnerStr)
                    }
                }
                // set the status if the match draw
                if (counter == 9 && flag == 0) {
                    _binding.tvResult.text = "Match Draw"
                    _binding.playAgain.visibility = View.VISIBLE
                }
            }
        } else
            Toast.makeText(context, "Please invite a friend", Toast.LENGTH_SHORT).show()
    }

    fun sendRadioMessage(msgStr: String) {
        if (lastMessageFromLocal)
            model.sendMessage(msgStr, contactKey)
    }

    fun messageReceived(messages: List<Packet>) {
        if (contactKey.isNotBlank()) {
            if (messages.size > 0) {
                val packet = messages[messages.size - 1]
                val msg = packet.data
                if (msg.status == MessageStatus.RECEIVED) {
                    val msgText = msg.text.toString()
                    ///Check if message is for invite, reset it, set user names
                    val inviteAcceptSplit = msgText.split("-")
                    if ((isInviteMessage(inviteAcceptSplit) && inviteAcceptSplit.size == 3)
                        || msgText.startsWith("playAgain")
                    ) {
                        val delayForAnimation = if (msgText.startsWith("playAgain")) {
                            _binding.coinFlipAnimation.visibility = View.VISIBLE
                            5000L
                        } else 0L
                        Handler(Looper.getMainLooper()).postDelayed({
                            _binding.coinFlipAnimation.visibility = View.GONE
                            gameInviteAcceptMsg = inviteAcceptSplit[1]
                            msgFrom = msg.from ?: ""
                            startGame(msgFrom, inviteAcceptSplit)
                        }, delayForAnimation)
                        return
                    }


                    // P1:2
                    val splitResult = msgText.split(":")

                    if (splitResult.size == 2) {
                        lastMessageFromLocal = packet.data.from == DataPacket.ID_LOCAL
                        if (!lastMessageFromLocal &&
                            lastReceivedMSG.equals(packet.data.text, true).not()
                        ) {
                            val firstPart = splitResult[0]
                            val secondPart = splitResult[1]

                            setActivePlayer(firstPart)
                            playerTap(secondPart.toInt())

                            lastReceivedMSG = packet.data.text
                        }
                    }

                } else {
//                    Log.d("Sufyan", "Message not delivered")
                }
            } else {
                Log.d("Sufyan", "Message size 0")
            }
        }

    }

    private var gameInviteAcceptMsg = "X"
    private var msgFrom = ""

    private fun startGame(msgFrom: String?, inviteAcceptSplit: List<String>) {
        _binding.coinFlipAnimation.visibility = View.GONE
        _binding.playAgain.visibility = View.GONE
        gameReset("")
        setPlayerName(inviteAcceptSplit, msgFrom)
        lockFirstMove(inviteAcceptSplit[2])
        Timber.e("StartingPlayerMove: ${inviteAcceptSplit[2]}")

    }

    private fun lockFirstMove(firstAttemptByPlayer: String) {
        if (!ownPlayerName.equals(firstAttemptByPlayer, true)) {
            lastMessageFromLocal = true
        }
    }

    private fun isInviteMessage(msgText: List<String>): Boolean {
        return msgText[0].contains(InviteState.INVITE_ACCEPTED.title, true)
    }

    fun String.switchTextBlueGreen(): String {
        var result = this
        result = result.replace("X", "Blue")
        result = result.replace("O", "Green")
        return result
    }


    // reset the game
    fun gameReset(resultMessage: String) {
        drawHorizontalWinState(-1)

        gameActive = true
        activePlayer = 0
        counter = 0
        lastMessageFromLocal = false
        //set all position to Null
        Arrays.fill(gameState, 2)

        _binding.tvResult.text = ""
        _binding.tvResult.text = resultMessage.ifBlank { "" }

        val list = List(matrixRowCol * matrixRowCol) { TickTackToeOptionState() }
        gamePlayAdapter.submitList(list)
    }

    private fun setActivePlayer(player: String) {
        activePlayer = if (player.equals(UIViewModel.Player.X.name, true))
            0
        else
            1
    }

    private val leftGameReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { _intent ->
                try {
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
    }

}