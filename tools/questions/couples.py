"""
Couples questions, written for this app: funny, romantic, spicy and deep.

    python tools/questions/couples.py

Original text. Nothing here is copied from a published list; the moods follow
what the research says builds closeness (turn-taking disclosure that deepens
gradually, responding to good news, appreciating specific acts, trying new
things together) without borrowing anyone's wording.

Voice follows DESIGN.md: plain, warm, no exclamation marks, no slang, no
coaching. "I" and "me" in a question mean whoever is answering; the other
person guesses. Spicy is warm, not explicit.
"""
import glob, io, json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "app", "src", "main", "assets"))
OUT = os.path.join(ASSETS, "daily")
TAGS = {"time", "words", "gifts", "acts", "touch", "planning", "money", "family",
        "friends", "home", "work", "past", "romance", "fun"}


def R(id, text, depth, tags): return dict(id=id, text=text, type="REFLECT", depth=depth, tags=tags)
def G(id, text, depth, tags): return dict(id=id, text=text, type="GUESS_ME", depth=depth, tags=tags)
def T(id, text, a, b, depth, tags): return dict(id=id, text=text, type="THIS_OR_THAT", options=[a, b], depth=depth, tags=tags)
def P(id, text, opts, depth, tags): return dict(id=id, text=text, type="PICK_ONE", options=opts, depth=depth, tags=tags)
def M(id, text, opts, depth, tags, cap): return dict(id=id, text=text, type="MULTI_SELECT", options=opts, maxChoices=cap, depth=depth, tags=tags)
def K(id, text, opts, depth, tags): return dict(id=id, text=text, type="RANK", options=opts, depth=depth, tags=tags)
def D(id, text, opts, depth, tags): return dict(id=id, text=text, type="DROPDOWN", options=opts, depth=depth, tags=tags)
def S(id, text, depth, tags, low, high): return dict(id=id, text=text, type="SCALE", lowLabel=low, highLabel=high, depth=depth, tags=tags)


FUNNY = [
    T("fun1", "If we were in a heist movie, I would be:", "The mastermind", "The one who trips the alarm", 1, ["fun"]),
    T("fun2", "Stuck in an elevator together, I would:", "Tell stories until help comes", "Press every button twice", 1, ["fun"]),
    T("fun3", "My dance moves are:", "Better than I let on", "Exactly as bad as you think", 1, ["fun"]),
    T("fun4", "In a zombie outbreak, we would last:", "Longer than anyone expects", "About eleven minutes", 1, ["fun"]),
    T("fun5", "When I lose at a board game, I:", "Shake hands like an adult", "Ask for a rematch immediately", 1, ["fun"]),
    T("fun6", "If our pet could talk, it would mostly complain about:", "Me", "You", 1, ["fun", "home"]),
    S("fun7", "How likely am I to talk to the TV during a show?", 1, ["fun", "home"], "Never", "Every scene"),
    S("fun8", "How good would I be at keeping a surprise party secret?", 1, ["fun", "words"], "Hopeless", "Vault"),
    S("fun9", "How dramatic am I when I have a small cold?", 1, ["fun"], "Barely notice", "Write my will"),
    S("fun10", "How competitive do I get at mini golf?", 1, ["fun"], "Just here for fun", "Keeping score in ink"),
    S("fun11", "How long could I survive without my phone?", 1, ["fun", "time"], "An hour", "A whole week"),
    P("fun12", "What would my reality show be called?", ["Snacks and Opinions", "Lost Again", "Five More Minutes", "Technically Correct"], 1, ["fun"]),
    P("fun13", "Which job would I be secretly great at?", ["Game show host", "Lifeguard", "Detective", "Wedding planner"], 1, ["fun", "work"]),
    P("fun14", "What would I name our boat?", ["Knot Tonight", "The Second Mortgage", "Sea You Later", "Just One More Trip"], 1, ["fun"]),
    P("fun15", "If I were a kitchen appliance, I would be:", ["A blender, loud and useful", "A toaster, reliable", "A slow cooker, worth the wait", "A microwave, done in a minute"], 1, ["fun", "home"]),
    P("fun16", "What is my most likely reason for being late?", ["Could not find my keys", "Got talking to someone", "Changed outfits", "Lost track of time"], 1, ["fun", "time"]),
    M("fun17", "Pick the things I would definitely do on a free Saturday with no plans.", ["Sleep in", "Start a project I will not finish", "Eat breakfast for lunch", "Rearrange something", "Watch an entire season of something"], 1, ["fun", "time"], 3),
    M("fun18", "Which of these would I be banned from at a party?", ["The karaoke machine", "The snack table", "The music", "The board games", "Telling that one story again"], 1, ["fun", "friends"], 2),
    M("fun19", "Pick the ones I pretend to be good at.", ["Parallel parking", "Remembering names", "Cooking without a recipe", "Directions", "Assembling furniture"], 1, ["fun", "home"], 3),
    M("fun20", "Which of these could I talk about for an hour?", ["A show I just finished", "Something at work", "Food", "A random fact I learned", "Why I was right about something"], 1, ["fun", "words"], 2),
    M("fun21", "Pick my superpowers, if I could only have two.", ["Never getting lost", "Always finding parking", "Instant naps", "Remembering every birthday", "Knowing what is for dinner"], 1, ["fun"], 2),
    K("fun22", "Put these in order of how fast I would get distracted.", ["A dog walking by", "A snack", "My phone", "A good song"], 1, ["fun"]),
    K("fun23", "Rank these by how likely I am to cry during them.", ["A dog movie", "A wedding", "A commercial", "Finishing a long book"], 1, ["fun"]),
    K("fun24", "Order these by how much I would complain about them.", ["A long line", "Cold coffee", "Slow wifi", "Wet socks"], 1, ["fun", "home"]),
    K("fun25", "Put these in order of what I would save first in a flood.", ["The good pillows", "Snacks", "The photos", "My phone charger"], 1, ["fun", "home"]),
    K("fun26", "Rank these by how badly I would do on a survival show.", ["Building a fire", "Finding food", "Keeping calm", "Sleeping outside"], 1, ["fun"]),
    D("fun27", "What would my theme song be?", ["Something with a big drum solo", "A soft piano ballad", "A cartoon opening", "Elevator music", "A marching band", "An old country song"], 1, ["fun"]),
    D("fun28", "What animal am I most like before coffee?", ["A bear", "A sloth", "A startled cat", "A goldfish", "A grumpy owl", "A puppy, somehow"], 1, ["fun"]),
    D("fun29", "Which decade would I thrive in?", ["The twenties", "The fifties", "The seventies", "The eighties", "The nineties", "Right now, thank you"], 1, ["fun", "past"]),
    D("fun30", "If I opened a restaurant, what would it serve?", ["Only breakfast", "Only desserts", "Soup, all kinds", "Whatever I feel like that day", "Grilled cheese, perfected", "Snacks, as a meal"], 1, ["fun"]),
    G("fun31", "What is the silliest thing I have ever gotten really upset about?", 1, ["fun", "past"]),
    G("fun32", "What would I do with a completely free day and a hundred dollars?", 1, ["fun", "time", "money"]),
    G("fun33", "What is the worst gift I have ever been given?", 1, ["fun", "gifts", "past"]),
    G("fun34", "What would I bring to a desert island if I could only bring one thing?", 1, ["fun"]),
    G("fun35", "What is a weird food combination I secretly love?", 1, ["fun"]),
    R("fun36", "What is the funniest thing that has happened to the two of us?", 1, ["fun", "past"]),
    R("fun37", "What is a habit of mine that used to annoy you and now makes you smile?", 1, ["fun", "home"]),
    R("fun38", "If we swapped lives for a day, what would you do first?", 1, ["fun"]),
    R("fun39", "What is a story about us that you love telling other people?", 1, ["fun", "past", "friends"]),
    R("fun40", "What is the most ridiculous argument we have ever had?", 1, ["fun", "past"]),
]

ROMANTIC = [
    T("rom1", "A perfect evening for us is:", "Dressed up and out", "Blanket, takeout, no plans", 1, ["romance", "time"]),
    T("rom2", "I would rather get:", "A love letter", "A surprise trip", 1, ["romance", "gifts"]),
    T("rom3", "The most romantic time of day is:", "Early morning", "Late at night", 1, ["romance", "time"]),
    T("rom4", "I fall for you more when you are:", "Being silly", "Being serious about something you love", 1, ["romance"]),
    T("rom5", "An anniversary should be:", "The same tradition every year", "Something new every year", 1, ["romance", "planning"]),
    T("rom6", "I would rather slow dance:", "In the kitchen", "At a wedding", 1, ["romance", "touch"]),
    S("rom7", "How much do I still get butterflies around you?", 2, ["romance"], "Rarely", "All the time"),
    S("rom8", "How much do I love little notes and texts from you during the day?", 1, ["romance", "words"], "Not my thing", "Keep them coming"),
    S("rom9", "How important is a real date night to me?", 1, ["romance", "time"], "Nice to have", "Essential"),
    S("rom10", "How much do I like being surprised by you?", 1, ["romance", "gifts"], "Please warn me", "Surprise me always"),
    S("rom11", "How sure was I about us in the first month?", 2, ["romance", "past"], "Not sure at all", "Completely sure"),
    P("rom12", "When did I first feel something real for you?", ["The first date", "A long conversation", "Meeting your friends", "Much later than you would think"], 2, ["romance", "past"]),
    P("rom13", "Which would make me feel most loved this week?", ["A long hug", "Hearing why you love me", "You handling something for me", "An evening with no phones"], 1, ["romance", "words", "touch", "acts", "time"]),
    P("rom14", "Which place would I pick for our next date?", ["Somewhere with a view", "Our favorite restaurant", "Somewhere we have never been", "Home, with the lights low"], 1, ["romance", "time"]),
    P("rom15", "What song would I pick for our first dance, if we did it again?", ["Something old and slow", "Something we can laugh to", "Whatever was playing when we met", "Something nobody else would pick"], 1, ["romance", "past"]),
    P("rom16", "Which of these would I most like to recreate?", ["Our first date", "Our first trip", "A night we stayed up talking", "The day we knew"], 2, ["romance", "past"]),
    M("rom17", "Pick the small things that make me feel loved.", ["You making my coffee", "A kiss hello", "You remembering something I said", "Holding hands in public", "You saving me the last bite"], 1, ["romance", "acts", "touch"], 3),
    M("rom18", "Which of these would I love for a surprise date?", ["A picnic", "Stargazing", "A cooking class", "A drive with no destination", "A concert"], 1, ["romance", "time", "fun"], 2),
    M("rom19", "Pick what I noticed first about you.", ["Your smile", "Your voice", "Your laugh", "Your eyes", "How you talked to other people"], 1, ["romance", "past"], 2),
    M("rom20", "Which of these would I keep forever, if you gave it to me?", ["A handwritten letter", "A photo of us", "A song you wrote or picked", "A small piece of jewelry", "A drawing"], 1, ["romance", "gifts"], 2),
    M("rom21", "Pick the moments I find romantic, even if nobody else would.", ["Doing chores together", "You falling asleep on me", "Grocery shopping side by side", "You singing badly", "A rainy day indoors"], 1, ["romance", "home"], 3),
    K("rom22", "Put these in order of what makes me feel closest to you.", ["A long talk", "A long hug", "Laughing together", "Doing something new together"], 2, ["romance", "words", "touch", "fun"]),
    K("rom23", "Rank these dates from most to least my kind of thing.", ["Candlelit dinner", "A hike and a view", "A movie night in", "A road trip"], 1, ["romance", "time"]),
    K("rom24", "Order these by how much I would want them on my birthday.", ["A thoughtful gift", "A day planned around me", "Kind words from you", "Quiet time together"], 1, ["romance", "gifts", "words", "time"]),
    K("rom25", "Rank these by how often I think about them.", ["How we met", "Our first kiss", "Our first trip", "A hard time we got through"], 2, ["romance", "past"]),
    D("rom26", "Which season feels most like us?", ["Early spring", "Long summer evenings", "Crisp fall", "Snowy winter", "The first warm day", "The last day of vacation"], 1, ["romance"]),
    D("rom27", "What would I say is our love song, if we had to pick one?", ["Something from when we met", "Something from a road trip", "Something from a wedding", "Something from a movie", "Something we sing badly together", "We do not have one yet"], 1, ["romance", "past"]),
    D("rom28", "Where would I want to renew our vows, or say them?", ["A beach", "A small chapel", "Our backyard", "The place we met", "A mountain top", "A courthouse, then a great dinner"], 2, ["romance", "planning"]),
    D("rom29", "What kind of love letter would I write?", ["Short and sweet", "Pages long", "Funny", "A list of reasons", "A poem, badly", "I would say it out loud instead"], 1, ["romance", "words"]),
    D("rom30", "Which of these would I call the most romantic gesture?", ["Remembering a tiny detail", "Planning a whole day", "Writing something by hand", "Showing up when it counts", "A long, slow kiss", "Carrying the bags without asking"], 1, ["romance", "acts", "words"]),
    G("rom31", "What is my favorite memory of the two of us?", 2, ["romance", "past"]),
    G("rom32", "What is one thing you do that makes me feel completely at home?", 2, ["romance", "home"]),
    G("rom33", "What did I think the first time I saw you?", 1, ["romance", "past"]),
    G("rom34", "What is a date I would love to go on that we have never tried?", 1, ["romance", "time", "planning"]),
    G("rom35", "What would I say is your most attractive quality that has nothing to do with looks?", 2, ["romance", "words"]),
    R("rom36", "When did you realize you were falling for me?", 2, ["romance", "past"]),
    R("rom37", "What is one small thing I did recently that made you feel loved?", 2, ["romance", "acts"]),
    R("rom38", "What is a romantic tradition you would like us to start?", 1, ["romance", "planning"]),
    R("rom39", "What do you love about us now that you did not expect at the start?", 3, ["romance", "past"]),
    R("rom40", "Where would you take me for a perfect day, and why there?", 1, ["romance", "time"]),
]

SPICY = [
    T("hot1", "The best kind of kiss is:", "Slow and unhurried", "Stolen in a hurry", 1, ["romance", "touch"]),
    T("hot2", "I find it more attractive when you:", "Take the lead", "Let me take the lead", 2, ["romance"]),
    T("hot3", "Flirting across a crowded room is:", "Thrilling", "Too much pressure", 1, ["romance"]),
    T("hot4", "A little jealousy is:", "Kind of flattering", "Not for me at all", 2, ["romance", "words"]),
    T("hot5", "I would rather be seduced with:", "Words", "Touch", 2, ["romance", "words", "touch"]),
    T("hot6", "Morning or night, when am I most in the mood?", "Morning", "Night", 2, ["romance", "time"]),
    S("hot7", "How much do I like being teased a little?", 2, ["romance", "words"], "Not at all", "A lot"),
    S("hot8", "How often would I like a real kiss, not a quick one?", 2, ["romance", "touch"], "Now and then", "Every day"),
    S("hot9", "How adventurous do I feel about trying something new together?", 2, ["romance", "fun"], "Keep it familiar", "Very curious"),
    S("hot10", "How much does it matter to me that you make the first move?", 2, ["romance"], "Not at all", "A lot"),
    S("hot11", "How confident do I feel in my own skin these days?", 2, ["romance", "words"], "Not very", "Very"),
    P("hot12", "What is the most attractive thing you can wear, to me?", ["Something dressed up", "Nothing fancy, just you", "Something of mine", "Whatever you wear when you feel good"], 1, ["romance"]),
    P("hot13", "Where would I most like to be kissed right now?", ["The neck", "The forehead", "The hand", "Wherever you choose"], 2, ["romance", "touch"]),
    P("hot14", "What sets the mood for me fastest?", ["A compliment", "A slow song", "Candles and quiet", "Being made to laugh"], 2, ["romance"]),
    P("hot15", "What is my favorite way to be pulled close?", ["A hand on my waist", "A hug from behind", "A hand in my hair", "Being pulled in to dance"], 2, ["romance", "touch"]),
    P("hot16", "Which flirt would work best on me?", ["A long look", "A whispered compliment", "A secret joke", "A note left somewhere I will find it"], 1, ["romance", "words"]),
    M("hot17", "Pick the ones that make me feel wanted.", ["You looking at me across a room", "A kiss that lasts", "You saying so", "You reaching for my hand", "You clearing an evening for us"], 2, ["romance", "touch", "words"], 3),
    M("hot18", "Which of these would I find attractive in you tonight?", ["Confidence", "A nice shirt", "Cooking for me", "Laughing at my jokes", "Taking charge of the evening"], 1, ["romance"], 3),
    M("hot19", "Pick the settings I find romantic in a spicy way.", ["A hotel room", "A thunderstorm", "A late-night drive", "A hot tub", "A cabin with a fireplace"], 2, ["romance", "planning"], 2),
    M("hot20", "Which of these would I like more of?", ["Kisses that go nowhere in a hurry", "Compliments about how I look", "Spontaneous moments", "Planned nights", "Massages"], 2, ["romance", "touch", "words"], 2),
    M("hot21", "Pick the ways I like to flirt back.", ["A look", "Teasing", "A touch on the arm", "A text", "Pretending not to notice"], 1, ["romance", "words"], 2),
    K("hot22", "Rank these by how much they get my attention.", ["Your voice low", "A kiss on the neck", "A look that lasts", "Your hand on my back"], 2, ["romance", "touch"]),
    K("hot23", "Put these in order of how romantic they would feel to me.", ["A bath for two", "Dancing in the dark", "Breakfast in bed", "Falling asleep tangled up"], 2, ["romance", "touch"]),
    K("hot24", "Order these by how much I would like them on a night away.", ["A great dinner", "A long bath", "No alarms", "A room with a view"], 2, ["romance", "planning"]),
    K("hot25", "Rank these by how much they make me want to pull you close.", ["You dressed up", "You laughing", "You being kind to someone", "You confident about something"], 2, ["romance"]),
    D("hot26", "Which would I pick for a weekend with no one else around?", ["A cabin in the woods", "A beach hotel", "A city apartment", "Our place with the phones off", "A bed and breakfast", "A tent under the stars"], 2, ["romance", "planning"]),
    D("hot27", "What is my idea of the perfect lazy morning with you?", ["Coffee in bed", "Staying under the covers", "A slow breakfast", "A shower that takes too long", "Reading side by side", "Talking until noon"], 2, ["romance", "time"]),
    D("hot28", "What kind of compliment makes me blush?", ["About how I look", "About how I think", "About how I make you feel", "About something specific I did", "Anything whispered", "Anything said in front of friends"], 1, ["romance", "words"]),
    D("hot29", "Which song would I put on to set the mood?", ["Something slow and old", "Something with a heavy beat", "Jazz", "Something from when we met", "Something in another language", "Silence works"], 2, ["romance"]),
    D("hot30", "Where would I most like to be kissed, of these places we have been?", ["In the rain", "At a concert", "On a beach", "In the kitchen", "At the top of a hill", "In the car before going inside"], 2, ["romance", "past"]),
    G("hot31", "What is one thing you do that I find attractive and have never told you?", 2, ["romance", "words"]),
    G("hot32", "What is my favorite part of you to look at?", 2, ["romance"]),
    G("hot33", "What kind of date would get me in the mood fastest?", 2, ["romance", "time"]),
    G("hot34", "What is something I have always wanted to try with you, but not said?", 3, ["romance", "words"]),
    G("hot35", "When did I last feel really desired by you?", 2, ["romance", "past"]),
    R("hot36", "What is the most attractive thing about me that I probably do not notice?", 2, ["romance", "words"]),
    R("hot37", "What is a moment between us you still think about?", 2, ["romance", "past"]),
    R("hot38", "What would make you feel more wanted by me?", 3, ["romance", "words", "touch"]),
    R("hot39", "What is a fantasy date, with no budget, that you would plan for us?", 2, ["romance", "planning"]),
    R("hot40", "What is one thing you would like me to do more often when we are alone?", 3, ["romance", "touch"]),
]

DEEP = [
    T("deep1", "When we fight, I need:", "Space first", "To talk it through right away", 2, ["words"]),
    T("deep2", "I feel most secure in us when:", "We are together", "We are apart and still fine", 3, ["words", "time"]),
    T("deep3", "It is harder for me to say:", "I am sorry", "I need help", 3, ["words"]),
    T("deep4", "When something is wrong, I would rather you:", "Ask me", "Wait until I bring it up", 2, ["words"]),
    T("deep5", "In ten years, I most want us to be:", "Settled and steady", "Still having adventures", 2, ["planning"]),
    T("deep6", "I show love more by:", "Doing things", "Saying things", 2, ["acts", "words"]),
    S("deep7", "How easy is it for me to ask you for what I need?", 3, ["words"], "Very hard", "Very easy"),
    S("deep8", "How understood have I felt by you lately?", 3, ["words"], "Not much", "Completely"),
    S("deep9", "How much do I worry about the future?", 2, ["planning"], "Hardly ever", "All the time"),
    S("deep10", "How much has being with you changed me?", 3, ["past"], "Not much", "Profoundly"),
    S("deep11", "How safe do I feel telling you something embarrassing?", 3, ["words"], "Not very", "Completely"),
    P("deep12", "What do I need most when I am having a hard week?", ["Someone to listen", "A distraction", "Practical help", "Time alone, then a hug"], 2, ["acts", "words", "touch", "time"]),
    P("deep13", "What am I most proud of about us?", ["How we handle hard things", "How we laugh", "What we have built", "That we still choose each other"], 3, ["past", "home"]),
    P("deep14", "Which of these do I find hardest to talk about?", ["Money", "Family", "The future", "What I feel"], 3, ["money", "family", "planning", "words"]),
    P("deep15", "What do I think we are best at as a couple?", ["Making each other laugh", "Getting through hard times", "Planning ahead", "Taking care of each other"], 2, ["acts", "fun", "planning"]),
    P("deep16", "What would I say keeps us close on busy weeks?", ["Small texts", "Eating together", "A hug before sleep", "Talking before bed"], 2, ["time", "touch", "words"]),
    M("deep17", "Pick the things I am quietly worried about right now.", ["Work", "Money", "Family", "Health", "Us"], 3, ["work", "money", "family"], 2),
    M("deep18", "Which of these did I learn from how my family did things?", ["How to argue", "How to show love", "How to handle money", "How to celebrate", "How to apologize"], 3, ["family", "past"], 3),
    M("deep19", "Pick what I would want you to know if I could not say it.", ["I am proud of you", "I am scared sometimes", "I need more time with you", "I am happy", "I am tired"], 3, ["words"], 2),
    M("deep20", "Which of these would I want our home to always feel like?", ["Calm", "Full of people", "Safe", "Playful", "A little messy and lived in"], 2, ["home"], 2),
    M("deep21", "Pick the moments I felt closest to you this year.", ["A hard day", "A trip", "A normal Tuesday", "A celebration", "A long talk"], 3, ["past", "time"], 2),
    K("deep22", "Put these in order of what I need most from you right now.", ["Patience", "Encouragement", "Honesty", "Affection"], 3, ["words", "touch"]),
    K("deep23", "Rank these by how much they matter to me in a long life together.", ["Trust", "Laughter", "Growth", "Shared goals"], 3, ["planning"]),
    K("deep24", "Order these by what I find hardest to forgive.", ["Being ignored", "Being lied to", "Being embarrassed in public", "A broken promise"], 3, ["words"]),
    K("deep25", "Rank these by what I would most want to be remembered for.", ["Kindness", "Courage", "Humor", "Loyalty"], 3, ["words"]),
    D("deep26", "What is the biggest thing I want to change about myself?", ["How I handle stress", "How I spend my time", "How I talk to myself", "How I show up for people", "How I handle money", "Nothing right now"], 3, ["words", "time", "money"]),
    D("deep27", "What do I most want our life to be about?", ["Family", "Adventure", "Comfort and peace", "Making something together", "Helping other people", "Each other, mostly"], 3, ["family", "planning"]),
    D("deep28", "Which of these would I say I am still healing from?", ["Something from childhood", "An old relationship", "A loss", "A disappointment", "A friendship ending", "Nothing I would name"], 3, ["past", "family"]),
    D("deep29", "What is my biggest fear about growing older?", ["Losing people", "Losing my health", "Regret", "Being bored", "Money", "I am not afraid of it"], 3, ["planning", "money"]),
    D("deep30", "When do I feel most like myself?", ["Outdoors", "With friends", "Working on something", "Alone", "With you", "Traveling"], 2, ["time", "friends"]),
    G("deep31", "What is something I have never fully forgiven myself for?", 3, ["past", "words"]),
    G("deep32", "What would I say is the hardest thing we have been through together?", 3, ["past"]),
    G("deep33", "What do I wish more people knew about me?", 3, ["words"]),
    G("deep34", "What dream have I quietly let go of?", 3, ["planning", "past"]),
    G("deep35", "What do I think makes us work, when so many couples do not?", 3, ["words"]),
    R("deep36", "What is something you have never told me because you were not sure how I would react?", 3, ["words"]),
    R("deep37", "When did you feel most supported by me?", 3, ["acts", "past"]),
    R("deep38", "What is one thing you want us to be braver about?", 3, ["planning"]),
    R("deep39", "What part of your childhood do you want our home to have, and what part should it never have?", 3, ["family", "home", "past"]),
    R("deep40", "What is a good thing that happened to you this week that you have not really told me about?", 2, ["words"]),
]

DECKS = [
    ("daily-us-funny", "Just for laughs", "PLAYFUL", "Playful", FUNNY),
    ("daily-us-romantic", "Still falling", "ROMANTIC", "Romantic", ROMANTIC),
    ("daily-us-spicy", "Turned up", "SPICY", "Spicy", SPICY),
    ("daily-us-deep", "Further in", "DEEP", "Deep", DEEP),
]

# ---- validation, mirroring QuizJson ----------------------------------------------
ours = {d[0] for d in DECKS}
existing_ids, existing_text = set(), set()
for f in glob.glob(os.path.join(ASSETS, "quizzes", "*.json")) + glob.glob(os.path.join(OUT, "*.json")):
    d = json.load(open(f, encoding="utf-8"))
    if d["id"] in ours:
        continue
    for q in d["questions"]:
        existing_ids.add(q["id"]); existing_text.add(q["text"].lower())

ids = set()
for deck_id, title, theme, category, qs in DECKS:
    assert len(qs) == 40, (deck_id, len(qs))
    for q in qs:
        assert q["id"] not in ids and q["id"] not in existing_ids, q["id"]
        ids.add(q["id"])
        assert q["text"].lower() not in existing_text, ("already asked", q["text"])
        assert "!" not in json.dumps(q), q["id"]
        t, opts = q["type"], q.get("options", [])
        if t == "THIS_OR_THAT": assert len(opts) == 2
        elif t in ("REFLECT", "GUESS_ME", "SCALE"): assert not opts
        else: assert len(opts) >= 2
        assert len({o.lower() for o in opts}) == len(opts), q
        if "maxChoices" in q: assert 1 <= q["maxChoices"] <= len(opts)
        assert set(q["tags"]) <= TAGS, q
        q["category"] = category

for deck_id, title, theme, category, qs in DECKS:
    deck = dict(id=deck_id, title=title, theme=theme, blurb="Daily only.",
                source="Written for Click Clack", questions=qs)
    with io.open(os.path.join(OUT, deck_id + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(deck, f, indent=2, ensure_ascii=False)
        f.write("\n")
print("wrote", sum(len(d[4]) for d in DECKS), "questions")
