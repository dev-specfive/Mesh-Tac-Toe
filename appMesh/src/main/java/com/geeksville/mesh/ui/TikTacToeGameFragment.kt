package com.geeksville.mesh.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.geeksville.mesh.DataPacket
import com.geeksville.mesh.MessageStatus
import com.geeksville.mesh.R
import com.geeksville.mesh.android.Logging
import com.geeksville.mesh.database.entity.Packet
import com.geeksville.mesh.databinding.FragmentTikTacToeGameBinding
import com.geeksville.mesh.model.UIViewModel
import com.geeksville.mesh.service.EXTRA_ACCEPTED_CHANNEL_KEY
import com.geeksville.mesh.service.EXTRA_INVITE_ACCEPTED_MSG_KEY
import com.geeksville.mesh.service.EXTRA_SENDER_MSG_KEY
import com.geeksville.mesh.service.InviteState
import java.util.Arrays


class TikTacToeGameFragment : ScreenFragment("TikTacToeGameFragment"), Logging {

    var gameActive = true

    // Player representation
    // 0 - X
    // 1 - O
    var activePlayer = 0
    var gameState = intArrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2)


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
    var counter = 0
    val TAG: String = TikTacToeGameFragment::class.java.simpleName

    private var _binding: FragmentTikTacToeGameBinding? = null
    private val model: UIViewModel by activityViewModels()
    private var contactKey = ""
    private lateinit var ownPlayerName: String
    private var lastMessageFromLocal: Boolean = false
    private val binding get() = _binding!!

    companion object {
        @JvmStatic
        fun newInstance() = TikTacToeGameFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTikTacToeGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observers()
        clickListeners()
        gameReset("")
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
                    inviteAcceptedReceiver,
                    IntentFilter(InviteState.INVITE_ACCEPTED.title)
                )
        }
    }

    override fun onStop() {
        super.onStop()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(inviteAcceptedReceiver)
        }
    }

    fun observers() {
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

    private fun setPlayerName(inviteMsg: List<String>, from: String?) {
        inviteMsg[1].apply {
            ownPlayerName = if (from == DataPacket.ID_LOCAL) {
                this
            } else {
                if (equals(UIViewModel.Player.X.name, true)) UIViewModel.Player.O.name
                else UIViewModel.Player.X.name

            }
            binding.tvPlayerName.text = String.format(getString(R.string.game_user), ownPlayerName)
            binding.tvPlayerName.visibility = View.VISIBLE
        }
        binding.tvResult.text = "${inviteMsg[2]}'s Turn - Tap to play"
        setActivePlayer(inviteMsg[2])
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun clickListeners() {
        binding.tv11.setOnClickListener {
            playIfOppenentMadeMove(it)
        }
        binding.tv12.setOnClickListener {
            playIfOppenentMadeMove(it)
        }
        binding.tv13.setOnClickListener {
            playIfOppenentMadeMove(it)
        }
        binding.tv21.setOnClickListener {
            playIfOppenentMadeMove(it)
        }
        binding.tv22.setOnClickListener {
            playIfOppenentMadeMove(it)
        }
        binding.tv23.setOnClickListener {
            playIfOppenentMadeMove(it)
        }
        binding.tv31.setOnClickListener {
            playIfOppenentMadeMove(it)
        }
        binding.tv32.setOnClickListener {
            playIfOppenentMadeMove(it)
        }
        binding.tv33.setOnClickListener {
            playIfOppenentMadeMove(it)
        }

    }

    private fun playIfOppenentMadeMove(view:View){
        if (!lastMessageFromLocal) {
            lastMessageFromLocal = true
            playerTap(view)
        }
        else
            Toast.makeText(context, "Please wait for the opponent's move", Toast.LENGTH_SHORT).show()
    }

    // this function will be called every time a
    // players tap in an empty box of the grid
    fun playerTap(view: View) {
        getInvitedChannelName()
        if (contactKey.isNotBlank()) {
            val textView = view as TextView
            val tappedImage = textView.tag.toString().toInt()

            // game reset function will be called
            // if someone wins or the boxes are full
            if (!gameActive) {
                gameReset("X's Turn - Tap to play")
                //Reset the counter
            }

            // if the tapped image is empty
            if (gameState[tappedImage] === 2) {
                // increase the counter
                // after every tap
                counter++

                // check if its the last box
                if (counter == 9) {
                    // reset the game
                    gameActive = false
                }

                // mark this position
                gameState[tappedImage] = activePlayer

                // this will give a motion
                // effect to the image
                textView.translationY = -1000f

                // change the active player

                // from 0 to 1 or 1 to 0
                if (activePlayer == 0) {
                    // set the image of x
                    textView.text = "X"
                    activePlayer = 1
                    val tempMsg = "X:${tappedImage}"
                    sendRadioMessage(tempMsg)
                    // change the status
                    binding.tvResult.text = "O's Turn - Tap to play"
                } else {
                    // set the image of o
                    textView.text = "O"
                    activePlayer = 0
                    val tempMsg = "O:${tappedImage}"
                    sendRadioMessage(tempMsg)
                    // change the status
                    binding.tvResult.text = "X's Turn - Tap to play"
                }
                textView.animate().translationYBy(1000f).setDuration(300)
            }
            else
                lastMessageFromLocal = false
            var flag = 0
            // Check if any player has won if counter is > 4 as min 5 taps are
            // required to declare a winner
            if (counter > 4) {
                for (winPosition in winPositions) {
                    if (gameState[winPosition[0]] === gameState[winPosition[1]] &&
                        gameState[winPosition[1]] === gameState[winPosition[2]] && gameState[winPosition[0]] !== 2) {
                        flag = 1

                        // Somebody has won! - Find out who!
                        var winnerStr: String

                        // game reset function be called
                        gameActive = false
                        winnerStr = if (gameState[winPosition[0]] === 0) {
                            "X has won"
                        } else {
                            "O has won"
                        }
                        // Update the status bar for winner announcement
                        binding.tvResult.text = winnerStr
                        gameReset(winnerStr)

                    }
                }
                // set the status if the match draw
                if (counter == 9 && flag == 0) {
                    gameReset( "Match Draw")
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
                    if (isInviteMessage(inviteAcceptSplit) && inviteAcceptSplit.size == 3){
                        gameReset("")
                        setPlayerName(inviteAcceptSplit, msg.from)
                        lockFirstMove(inviteAcceptSplit[2])
                        return
                    }

                    // P1:2
                    val splitResult = msgText.split(":")
//                    Toast.makeText(context, "$gameActive", Toast.LENGTH_SHORT).show()

                    if (splitResult.size == 2) {
                        lastMessageFromLocal = packet.data.from == DataPacket.ID_LOCAL
                        if (!lastMessageFromLocal){
                            val firstPart = splitResult[0]
                            val secondPart = splitResult[1]

                            setActivePlayer(firstPart)
                            when (secondPart) {
                                '0'.toString() -> {
                                    playerTap(binding.tv11)
                                }
                                '1'.toString() -> {
                                    playerTap(binding.tv12)
                                }
                                '2'.toString() -> {
                                    playerTap(binding.tv13)
                                }
                                '3'.toString() -> {
                                    playerTap(binding.tv21)
                                }
                                '4'.toString() -> {
                                    playerTap(binding.tv22)
                                }
                                '5'.toString() -> {
                                    playerTap(binding.tv23)
                                }
                                '6'.toString() -> {
                                    playerTap(binding.tv31)
                                }
                                '7'.toString() -> {
                                    playerTap(binding.tv32)
                                }
                                '8'.toString() -> {
                                    playerTap(binding.tv33)
                                }
                            }
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

    private fun lockFirstMove(firstAttemptByPlayer: String) {
        if (!ownPlayerName.equals(firstAttemptByPlayer, true)){
            lastMessageFromLocal = true
        }
    }

    private fun isInviteMessage(msgText: List<String>):Boolean {
        return msgText[0].contains(InviteState.INVITE_ACCEPTED.title,true)
    }


    // reset the game
    fun gameReset(resultMessage:String) {
        gameActive = true
//        activePlayer = 0
        if (::ownPlayerName.isInitialized)
            setActivePlayer(ownPlayerName)
        counter = 0
        lastMessageFromLocal = false
        //set all position to Null
        Arrays.fill(gameState, 2)
        // remove all the images from the boxes inside the grid
        binding.tv11.text = ""
        binding.tv12.text = ""
        binding.tv13.text = ""
        binding.tv21.text = ""
        binding.tv22.text = ""
        binding.tv23.text = ""
        binding.tv31.text = ""
        binding.tv32.text = ""
        binding.tv33.text = ""
        binding.tvResult.text = ""

        binding.tvResult.text = resultMessage.ifBlank { "" }
    }
    private fun setActivePlayer(player: String) {
        activePlayer = if (player.equals(UIViewModel.Player.X.name, true))
            0
        else
            1
    }

    private val inviteAcceptedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                _intent ->
                val acceptedChannel = _intent.getStringExtra(EXTRA_ACCEPTED_CHANNEL_KEY)
                val sender = _intent.getStringExtra(EXTRA_SENDER_MSG_KEY)
                val inviteAcceptedMsg = _intent.getStringExtra(EXTRA_INVITE_ACCEPTED_MSG_KEY)
                if (!acceptedChannel.isNullOrBlank() && !sender.isNullOrBlank() && !inviteAcceptedMsg.isNullOrBlank()){
                    val msg = inviteAcceptedMsg.split("-")
                    model.setAccepted(acceptedChannel, true)
                    model.setContactKey(acceptedChannel)
                    gameReset("")
                    setPlayerName(msg, sender)
                    lockFirstMove(firstAttemptByPlayer = msg[2])
                }
            }

        }
    }
}