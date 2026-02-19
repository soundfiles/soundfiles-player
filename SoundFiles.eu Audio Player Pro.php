<?php
/**
 * Plugin Name: SoundFiles.eu Player Pro 
 * Description: Wer  v5.0  
 * Version: 5.0
 * Author: SoundFiles
 */

if (!defined('ABSPATH')) exit;

// 1. Act  
register_activation_hook(__FILE__, function() {
    add_option('sfp_global_color', '#105b81');
    add_option('sfp_global_width', '800px');
    add_option('sfp_global_download', 'yes');
    add_option('sfp_global_btn_text', 'Download');
});

// 2. Style  
add_action('wp_head', function() {
    echo '<style>
        .sfp-universal-outer-container {
            display: flex !important;
            flex-direction: column !important;
            align-items: center !important;
            justify-content: center !important;
            text-align: center !important;
            width: 100% !important;
            margin: 40px 0 !important;
            clear: both !important;
            float: none !important;
        }
        .sfp-player-dynamic-box {
            display: block !important;
            margin: 0 auto !important;
            float: none !important;
            height: auto !important;
            min-height: 120px !important;
        }
        .sfp-download-button-row {
            margin-top: 25px !important;
            display: block !important;
            width: 100% !important;
        }
        .sfp-btn-style {
            text-decoration: none !important;
            border-radius: 6px !important;
            font-family: sans-serif !important;
            font-weight: bold !important;
            display: inline-block !important;
            transition: transform 0.2s ease !important;
        }
        .sfp-btn-style:hover { transform: scale(1.05); }
    </style>';
});

// 3. Engine
function sfp_render_player_engine($file_id, $color = '', $width = '', $btn_text = '') {
    $file_id = esc_attr($file_id);
    $color = $color ?: get_option('sfp_global_color', '#105b81');
    $width = $width ?: get_option('sfp_global_width', '800px');
    $show_dl = get_option('sfp_global_download', 'yes');
    $btn_text = $btn_text ?: get_option('sfp_global_btn_text', 'POBIERZ PLIK');
    $file_url = "https://soundfiles.eu/" . $file_id . "/file";

    $js_width_clean = (int) filter_var($width, FILTER_SANITIZE_NUMBER_INT);
    $js_val = ($js_width_clean > 0) ? '"' . $js_width_clean . '"' : '"100%"';

    ob_start(); ?>
    <div class="sfp-universal-outer-container">
        <div class="sfp-player-dynamic-box" style="width: <?php echo $width; ?> !important; max-width: 95% !important;">
            
            <script type="text/javascript"> 
                var sffile = "<?php echo $file_id; ?>";  
                var sfplay = "<?php echo $color; ?>"; 
                var sfwave = "<?php echo $color; ?>"; 
                var sfwidth = <?php echo $js_val; ?>; 
                var sfborder = "<?php echo $color; ?>";  
                var sftext = "<?php echo $color; ?>"; 
            </script>
            <script type="text/javascript" src="https://soundfiles.eu/embed/embed_sf.js"></script>

            <?php if ($show_dl === 'yes') : ?>
            <div class="sfp-download-button-row">
                <a href="<?php echo esc_url($file_url); ?>" 
                   target="_blank" 
                   class="sfp-btn-style"
                   style="background-color: <?php echo $color; ?> !important; color: #ffffff !important; padding: 14px 40px !important;">
                   <?php echo esc_html($btn_text); ?>
                </a>
            </div>
            <?php endif; ?>
        </div>
    </div>
    <?php
    return ob_get_clean();
}

// 4. shortcode
add_filter('the_content', function($content) {
    $regex = '/https:\/\/soundfiles\.eu\/([a-zA-Z0-9]+)\/file/';
    return preg_replace_callback($regex, function($matches) {
        return sfp_render_player_engine($matches[1]);
    }, $content);
});

add_shortcode('soundfile', function($atts) {
    $a = shortcode_atts(array('id' => '', 'color' => '', 'width' => '', 'text' => ''), $atts);
    return !empty($a['id']) ? sfp_render_player_engine($a['id'], $a['color'], $a['width'], $a['text']) : '';
});

// 5. Panel admin
add_action('admin_menu', function() {
    add_options_page('SoundFiles Settings', 'SoundFiles Player', 'manage_options', 'sfp-settings', 'sfp_settings_view');
});

function sfp_settings_view() {
    if (isset($_POST['sfp_save'])) {
        update_option('sfp_global_color', $_POST['sfp_color']);
        update_option('sfp_global_width', $_POST['sfp_width']);
        update_option('sfp_global_btn_text', $_POST['sfp_btn_text']);
        update_option('sfp_global_download', isset($_POST['sfp_show_dl']) ? 'yes' : 'no');
        echo '<div class="updated"><p>Saved!</p></div>';
    }
    ?>
    <div class="wrap">
        <h1>SoundFiles Player</h1>
        <form method="post">
            <table class="form-table">
                <tr>
                    <th>Color:</th>
                    <td><input type="color" name="sfp_color" value="<?php echo get_option('sfp_global_color'); ?>"></td>
                </tr>
                <tr>
                    <th>Width:</th>
                    <td><input type="text" name="sfp_width" value="<?php echo get_option('sfp_global_width'); ?>" placeholder="np. 800px"></td>
                </tr>
                <tr>
                    <th>Download button text:</th>
                    <td><input type="text" name="sfp_btn_text" value="<?php echo get_option('sfp_global_btn_text'); ?>" class="regular-text"></td>
                </tr>
                <tr>
                    <th>Download button:</th>
                    <td><input type="checkbox" name="sfp_show_dl" value="1" <?php checked(get_option('sfp_global_download'), 'yes'); ?>></td>
                </tr>
            </table>
            <input type="submit" name="sfp_save" class="button button-primary" value="Save">
        </form>
    </div>
    <?php
}
