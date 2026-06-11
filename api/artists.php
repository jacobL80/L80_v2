<?php
// Upload this file to: public_html/api/artists.php

header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-Edit-Token');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/config.php';

try {
    $pdo = new PDO(
        'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
        DB_USER,
        DB_PASS,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database connection failed']);
    exit;
}

function verify_auth() {
    $token = $_SERVER['HTTP_X_EDIT_TOKEN'] ?? '';
    if ($token !== EDIT_SECRET) {
        http_response_code(401);
        echo json_encode(['error' => 'Unauthorized']);
        exit;
    }
}

function row_to_artist(array $row): array {
    return [
        'id'                  => (int) $row['id'],
        'name'                => $row['name'],
        'lastRelease'         => $row['last_release'],
        'nextRelease'         => $row['next_release'],
        'albumTitle'          => $row['album_title'],
        'confirmed'           => (bool) $row['confirmed'],
        'incompleteCollection'=> (bool) $row['incomplete_collection'],
        'notes'               => $row['notes'],
        'url'                 => $row['url'],
    ];
}

$method = $_SERVER['REQUEST_METHOD'];
$id     = isset($_GET['id']) ? (int) $_GET['id'] : null;

if ($method === 'GET') {
    $rows = $pdo->query('SELECT * FROM artists ORDER BY id')->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode(array_map('row_to_artist', $rows));

} elseif ($method === 'POST') {
    verify_auth();
    $body = json_decode(file_get_contents('php://input'), true);
    $stmt = $pdo->prepare(
        'INSERT INTO artists (name, last_release, next_release, album_title, confirmed, incomplete_collection, notes, url)
         VALUES (:name, :lr, :nr, :at, :conf, :ic, :notes, :url)'
    );
    $stmt->execute([
        'name'  => trim($body['name'] ?? ''),
        'lr'    => trim($body['lastRelease'] ?? ''),
        'nr'    => trim($body['nextRelease'] ?? ''),
        'at'    => trim($body['albumTitle'] ?? ''),
        'conf'  => empty($body['confirmed']) ? 0 : 1,
        'ic'    => empty($body['incompleteCollection']) ? 0 : 1,
        'notes' => trim($body['notes'] ?? ''),
        'url'   => trim($body['url'] ?? ''),
    ]);
    $newId = (int) $pdo->lastInsertId();
    $row = $pdo->query("SELECT * FROM artists WHERE id = $newId")->fetch(PDO::FETCH_ASSOC);
    http_response_code(201);
    echo json_encode(row_to_artist($row));

} elseif ($method === 'PUT' && $id) {
    verify_auth();
    $body = json_decode(file_get_contents('php://input'), true);
    $stmt = $pdo->prepare(
        'UPDATE artists SET name=:name, last_release=:lr, next_release=:nr, album_title=:at,
         confirmed=:conf, incomplete_collection=:ic, notes=:notes, url=:url WHERE id=:id'
    );
    $stmt->execute([
        'name'  => trim($body['name'] ?? ''),
        'lr'    => trim($body['lastRelease'] ?? ''),
        'nr'    => trim($body['nextRelease'] ?? ''),
        'at'    => trim($body['albumTitle'] ?? ''),
        'conf'  => empty($body['confirmed']) ? 0 : 1,
        'ic'    => empty($body['incompleteCollection']) ? 0 : 1,
        'notes' => trim($body['notes'] ?? ''),
        'url'   => trim($body['url'] ?? ''),
        'id'    => $id,
    ]);
    $row = $pdo->query("SELECT * FROM artists WHERE id = $id")->fetch(PDO::FETCH_ASSOC);
    echo json_encode(row_to_artist($row));

} elseif ($method === 'DELETE' && $id) {
    verify_auth();
    $pdo->prepare('DELETE FROM artists WHERE id = ?')->execute([$id]);
    echo json_encode(['success' => true]);

} else {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
}
