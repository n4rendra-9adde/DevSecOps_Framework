const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

app.get('/api/health', (req, res) => {
    res.json({ status: 'UP', service: 'nodejs-express-sample' });
});

app.get('/api/info', (req, res) => {
    res.json({
        name: 'DevSecOps Node.js Sample',
        version: '1.0.0',
        language: 'nodejs',
        framework: 'express'
    });
});

app.get('/api/items', (req, res) => {
    res.json([
        { id: 1, name: 'Item A' },
        { id: 2, name: 'Item B' }
    ]);
});

if (require.main === module) {
    app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
}

module.exports = app;
