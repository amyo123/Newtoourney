const express = require('express');
const cors = require('cors');
const multer = require('multer');
const cloudinary = require('cloudinary').v2;
const streamifier = require('streamifier');

const app = express();
app.use(cors());

// Configure multer to use memory storage
const storage = multer.memoryStorage();
const upload = multer({ storage: storage });

app.post('/api/upload', upload.single('file'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No file provided' });
    }

    if (!process.env.CLOUDINARY_URL) {
        return res.status(500).json({ error: 'CLOUDINARY_URL is not configured on the server' });
    }

    const uploadStream = cloudinary.uploader.upload_stream(
        { folder: 'app_uploads' },
        (error, result) => {
            if (error) {
                console.error("Cloudinary Upload Error:", error);
                return res.status(500).json({ error: error.message });
            }
            res.json({ secure_url: result.secure_url });
        }
    );

    streamifier.createReadStream(req.file.buffer).pipe(uploadStream);
});

app.get('/api/health', (req, res) => {
    res.json({ status: 'ok' });
});

module.exports = app;
