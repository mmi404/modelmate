-- Disabled system account that owns seed models (login impossible: hash is not BCrypt).
insert into users (first_name, last_name, email, password_hash, role)
values ('ModelMate', 'System', 'system@modelmate.local', '!disabled', 'USER');

-- ---------------------------------------------------------------------------
-- Categories (12)
-- ---------------------------------------------------------------------------
insert into categories (name, slug, description, applications) values
('Natural Language Processing', 'natural-language-processing',
 'Focuses on the interaction between computers and human language. NLP models understand, generate, and translate text.',
 'Chatbots, sentiment analysis, machine translation, summarization, question answering, content generation.'),
('Computer Vision', 'computer-vision',
 'Enables machines to interpret and understand visual information such as images and video.',
 'Object detection, image segmentation, facial recognition, medical imaging, OCR, autonomous vehicles.'),
('Speech and Audio Processing', 'speech-and-audio-processing',
 'Analyzing, transcribing, generating, or understanding audio and speech signals.',
 'Voice assistants, transcription, audio classification, speech synthesis, speaker recognition.'),
('Multimodal Models', 'multimodal-models',
 'Handle and integrate multiple input/output types such as text, images, and audio simultaneously.',
 'Visual question answering, caption generation, audio-visual chatbots, cross-modal retrieval.'),
('Recommendation Systems', 'recommendation-systems',
 'Suggest relevant items to users based on preferences, history, and behavior.',
 'Product recommendations, content suggestions, personalized feeds, music and movie platforms.'),
('Generative Models', 'generative-models',
 'Generate new data similar to a training set across text, image, audio, and video.',
 'Text generation, image synthesis, code generation, music composition, video generation.'),
('Time-Series Forecasting', 'time-series-forecasting',
 'Analyze temporal data to predict future values from historical patterns.',
 'Stock price prediction, weather forecasting, sales forecasting, anomaly detection.'),
('Robotics and Control', 'robotics-and-control',
 'Using AI to control mechanical systems and robots, often with real-time decision-making.',
 'Motion planning, manipulation, navigation, industrial automation, autonomous drones.'),
('Reinforcement Learning', 'reinforcement-learning',
 'Agents learn optimal behavior by interacting with an environment and maximizing reward.',
 'Game playing, robotics control, resource optimization, recommendation, autonomous agents.'),
('Anomaly Detection', 'anomaly-detection',
 'Identify rare items, events, or observations that differ significantly from the majority of data.',
 'Fraud detection, network intrusion detection, fault detection, quality control, monitoring.'),
('Graph and Network Models', 'graph-and-network-models',
 'Operate on graph-structured data, learning from nodes, edges, and their relationships.',
 'Social network analysis, molecule property prediction, fraud rings, knowledge graphs, routing.'),
('Code and Programming Assistants', 'code-and-programming-assistants',
 'Assist with software development tasks such as writing, completing, and debugging code.',
 'Code generation, bug fixing, code completion, test generation, documentation helpers.');

-- ---------------------------------------------------------------------------
-- Seed models (all APPROVED, owned by the system account)
-- ---------------------------------------------------------------------------
insert into models (name, slug, creator, category_id, description, website_url, status, submitted_by, approved_at)
select m.name, m.slug, m.creator, c.id, m.description, m.website_url, 'APPROVED', u.id, now()
from (values
    ('GPT-4', 'gpt-4', 'OpenAI', 'natural-language-processing',
     'A large multimodal language model that understands text and images, more accurate and creative than GPT-3.5.',
     'https://openai.com/research/gpt-4'),
    ('BERT', 'bert', 'Google AI', 'natural-language-processing',
     'A bidirectional transformer pre-trained with masked language modeling; strong on many NLP tasks.',
     'https://huggingface.co/google-bert/bert-base-uncased'),
    ('LLaMA', 'llama', 'Meta AI', 'natural-language-processing',
     'A family of open foundation models optimized for efficiency and performance across sizes and tasks.',
     'https://www.llama.com/'),
    ('YOLOv8', 'yolov8', 'Ultralytics', 'computer-vision',
     'The latest YOLO family model for real-time object detection, classification, and segmentation.',
     'https://yolov8.com/'),
    ('Vision Transformer (ViT)', 'vision-transformer-vit', 'Google Research', 'computer-vision',
     'Applies transformers directly to image patches for classification; inspired many CV variants.',
     'https://github.com/google-research/vision_transformer/'),
    ('Segment Anything Model (SAM)', 'segment-anything-model-sam', 'Meta AI', 'computer-vision',
     'A promptable zero-shot image segmentation model trained on 1.1B masks over 11M images.',
     'https://github.com/facebookresearch/segment-anything'),
    ('Whisper', 'whisper', 'OpenAI', 'speech-and-audio-processing',
     'A general-purpose speech recognition model that transcribes and translates audio in many languages.',
     'https://github.com/openai/whisper'),
    ('DeepSpeech', 'deepspeech', 'Mozilla', 'speech-and-audio-processing',
     'An open-source speech-to-text engine based on Baidu''s Deep Speech research.',
     'https://github.com/mozilla/DeepSpeech'),
    ('Bark', 'bark', 'Suno', 'speech-and-audio-processing',
     'A high-fidelity text-to-speech model with expressive voice generation in multiple languages.',
     'https://github.com/suno-ai/bark'),
    ('GPT-4o', 'gpt-4o', 'OpenAI', 'multimodal-models',
     'A multimodal model that understands and generates text, audio, and images together.',
     'https://openai.com/research/gpt-4o'),
    ('CLIP', 'clip', 'OpenAI', 'multimodal-models',
     'Aligns images and text with contrastive learning, enabling powerful zero-shot vision-language tasks.',
     'https://github.com/openai/CLIP'),
    ('Flamingo', 'flamingo', 'DeepMind', 'multimodal-models',
     'A few-shot visual-language model that processes interleaved sequences of images and text.',
     'https://deepmind.com/research/publications/flamingo')
) as m(name, slug, creator, category_slug, description, website_url)
join categories c on c.slug = m.category_slug
cross join (select id from users where email = 'system@modelmate.local') u;
